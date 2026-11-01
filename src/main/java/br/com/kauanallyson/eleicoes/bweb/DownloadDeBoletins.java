/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Garante que {@code bweb/} tem o {@code UF.csv} de cada UF publicada pelo TSE: baixa só o que falta, depois de
 * mostrar o tamanho e pedir confirmação. Execuções seguintes, com tudo presente, não baixam nada.
 */
public class DownloadDeBoletins {

    /** Boletins descompactados em UTF-8 ocupam ~6,3x o tamanho dos .zip (144 MB → ~900 MB no 2º turno de 2026). */
    private static final double DESCOMPACTADO_POR_COMPACTADO = 6.3;
    private static final int TENTATIVAS = 5;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Path diretorio;
    private final CatalogoTse catalogo;
    private final Confirmacao confirmacao;
    private final HttpClient http;
    private final Consumer<String> log;
    private final PreparadorDeBoletins preparador;

    public DownloadDeBoletins(Path diretorio, CatalogoTse catalogo, Confirmacao confirmacao, HttpClient http,
                              Consumer<String> log) {
        this.diretorio = diretorio;
        this.catalogo = catalogo;
        this.confirmacao = confirmacao;
        this.http = http;
        this.log = mensagem -> log.accept("[dados] " + mensagem);
        this.preparador = new PreparadorDeBoletins(diretorio);
    }

    /** @return {@code false} se faltavam boletins e o usuário não autorizou o download */
    public boolean garantir() throws IOException, InterruptedException {
        var presentes = new HashSet<>(Files.isDirectory(diretorio) ? new VotoReader(diretorio).ufs() : List.of());
        var faltando = catalogo.pacotes().stream().filter(p -> !presentes.contains(p.uf())).toList();
        if (faltando.isEmpty()) {
            log.accept("boletins de urna de %d UFs já estão em %s".formatted(presentes.size(), diretorio));
            return true;
        }

        long total = faltando.stream().mapToLong(PacoteTse::tamanho).sum();
        log.accept("%s não tem os boletins de urna de %d UF(s): %s".formatted(diretorio, faltando.size(),
                String.join(", ", faltando.stream().map(PacoteTse::uf).toList())));
        log.accept("fonte: Portal de Dados Abertos do TSE");
        log.accept("download: %s (%d arquivos .zip); espaço em disco depois de descompactar: ~%s".formatted(
                tamanho(total), faltando.size(), tamanho((long) (total * DESCOMPACTADO_POR_COMPACTADO))));

        if (!confirmacao.confirmar("Baixar agora?")) {
            log.accept("download não autorizado; nada foi baixado (sem terminal interativo, rode com --sim)");
            return false;
        }

        var temporarios = Files.createDirectories(diretorio.resolve(".download"));
        for (int i = 0; i < faltando.size(); i++) {
            var pacote = faltando.get(i);
            log.accept("(%d/%d) %s  %s  %s ...".formatted(i + 1, faltando.size(), pacote.uf(), pacote.nomeDoArquivo(),
                    tamanho(pacote.tamanho())));
            var zip = temporarios.resolve(pacote.nomeDoArquivo());
            baixar(pacote, zip);
            conferirHash(pacote, zip);
            preparador.preparar(zip, pacote.uf());
            Files.delete(zip);
            log.accept("(%d/%d) %s ok: SHA-512 confere, convertido para UTF-8 em %s".formatted(i + 1, faltando.size(),
                    pacote.uf(), diretorio.resolve(pacote.uf() + ".csv")));
        }
        try (var sobras = Files.list(temporarios)) {
            if (sobras.findAny().isEmpty()) Files.delete(temporarios);
        }
        log.accept("%d UF(s) prontas em %s".formatted(faltando.size(), diretorio));
        return true;
    }

    /** Retoma de onde parou (Range) se a conexão cair no meio. */
    private void baixar(PacoteTse pacote, Path destino) throws IOException, InterruptedException {
        IOException ultimoErro = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            long jaBaixado = Files.exists(destino) ? Files.size(destino) : 0;
            if (pacote.tamanho() > 0 && jaBaixado == pacote.tamanho()) return;

            var pedido = HttpRequest.newBuilder(pacote.zip()).GET();
            if (jaBaixado > 0) pedido.header("Range", "bytes=" + jaBaixado + "-");
            try {
                var resposta = http.send(pedido.build(), HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream corpo = resposta.body()) {
                    switch (resposta.statusCode()) {
                        case 206 -> anexar(corpo, destino);
                        case 200 -> Files.copy(corpo, destino, StandardCopyOption.REPLACE_EXISTING);
                        default -> throw new IOException("TSE respondeu " + resposta.statusCode() + " para " + pacote.zip());
                    }
                }
                if (pacote.tamanho() < 0 || Files.size(destino) == pacote.tamanho()) return;
                ultimoErro = new IOException("download incompleto de " + pacote.zip());
            } catch (IOException e) {
                ultimoErro = e;
            }
            log.accept("%s: conexão interrompida, retomando (tentativa %d de %d)".formatted(pacote.uf(), tentativa + 1, TENTATIVAS));
        }
        throw ultimoErro;
    }

    private static void anexar(InputStream corpo, Path destino) throws IOException {
        try (var saida = Files.newOutputStream(destino, StandardOpenOption.APPEND)) {
            corpo.transferTo(saida);
        }
    }

    /** O .sha512 do TSE tem o formato do sha512sum: "{@code <hash>  <arquivo>}". */
    private void conferirHash(PacoteTse pacote, Path zip) throws IOException, InterruptedException {
        var resposta = http.send(HttpRequest.newBuilder(pacote.sha512()).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() != 200) {
            throw new IOException("TSE respondeu " + resposta.statusCode() + " para " + pacote.sha512());
        }
        var esperado = resposta.body().trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        var obtido = sha512(zip);
        if (!obtido.equals(esperado)) {
            Files.delete(zip);
            throw new IOException("SHA-512 de " + pacote.nomeDoArquivo() + " não confere com o publicado pelo TSE");
        }
    }

    private static String sha512(Path arquivo) throws IOException {
        try (var entrada = Files.newInputStream(arquivo)) {
            var digest = MessageDigest.getInstance("SHA-512");
            var buffer = new byte[1 << 16];
            for (int lidos; (lidos = entrada.read(buffer)) > 0; ) digest.update(buffer, 0, lidos);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String tamanho(long bytes) {
        if (bytes < 0) return "tamanho desconhecido";
        if (bytes >= 1_000_000_000) return String.format(PT_BR, "%.1f GB", bytes / 1e9);
        return String.format(PT_BR, "%.1f MB", bytes / 1e6);
    }
}
