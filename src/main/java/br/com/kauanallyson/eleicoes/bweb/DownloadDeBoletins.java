/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Garante que a pasta tem o {@code UF.csv} de cada UF publicada pelo TSE: baixa só o que falta, depois de
 * mostrar o tamanho e pedir confirmação. Quando a pasta fica completa, grava um marcador e as execuções
 * seguintes não consultam mais o TSE (funcionam sem internet).
 */
public class DownloadDeBoletins {

    /** Boletins descompactados em UTF-8 ocupam ~6,3x o tamanho dos .zip (144 MB → ~900 MB no 2º turno de 2026). */
    private static final double DESCOMPACTADO_POR_COMPACTADO = 6.3;
    private static final String MARCADOR = ".boletins-completos";
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Path diretorio;
    private final PortalTse portal;
    private final Confirmation confirmation;
    private final Consumer<String> log;
    private final PreparadorDeBoletins preparador;

    public DownloadDeBoletins(Path diretorio, PortalTse portal, Confirmation confirmation, Consumer<String> log) {
        this.diretorio = diretorio;
        this.portal = portal;
        this.confirmation = confirmation;
        this.log = mensagem -> log.accept("[dados] " + mensagem);
        this.preparador = new PreparadorDeBoletins(diretorio);
    }

    /** @return {@code false} se faltavam boletins e o usuário não autorizou o download */
    public boolean garantir() throws IOException, InterruptedException {
        var presentes = new HashSet<>(Files.isDirectory(diretorio) ? new VotoReader(diretorio).ufs() : List.of());
        if (Files.exists(diretorio.resolve(MARCADOR))) {
            log.accept("boletins de urna de %d UFs já estão em %s".formatted(presentes.size(), diretorio));
            return true;
        }

        var faltando = new ArrayList<PacoteTse>();
        for (var pacote : portal.pacotes()) {
            if (!presentes.contains(pacote.uf())) faltando.add(portal.comTamanho(pacote));
        }
        if (faltando.isEmpty()) {
            log.accept("boletins de urna de %d UFs já estão em %s".formatted(presentes.size(), diretorio));
            marcarCompleto();
            return true;
        }

        long total = faltando.stream().mapToLong(PacoteTse::tamanho).sum();
        log.accept("%s não tem os boletins de urna de %d UF(s): %s".formatted(diretorio, faltando.size(),
                String.join(", ", faltando.stream().map(PacoteTse::uf).toList())));
        log.accept("fonte: Portal de Dados Abertos do TSE");
        log.accept("download: %s (%d arquivos .zip); espaço em disco depois de descompactar: ~%s".formatted(
                tamanho(total), faltando.size(), tamanho((long) (total * DESCOMPACTADO_POR_COMPACTADO))));

        if (!confirmation.confirm("Baixar agora?")) {
            log.accept("download não autorizado; nada foi baixado (sem terminal interativo, rode com --sim)");
            return false;
        }

        var temporarios = Files.createDirectories(diretorio.resolve(".download"));
        for (int i = 0; i < faltando.size(); i++) {
            var pacote = faltando.get(i);
            log.accept("(%d/%d) %s  %s  %s ...".formatted(i + 1, faltando.size(), pacote.uf(), pacote.nomeDoArquivo(),
                    tamanho(pacote.tamanho())));
            var zip = temporarios.resolve(pacote.nomeDoArquivo());
            portal.baixar(pacote, zip);
            preparador.preparar(zip, pacote.uf());
            Files.delete(zip);
            log.accept("(%d/%d) %s ok: SHA-512 confere, convertido para UTF-8 em %s".formatted(i + 1, faltando.size(),
                    pacote.uf(), diretorio.resolve(pacote.uf() + ".csv")));
        }
        try (var sobras = Files.list(temporarios)) {
            if (sobras.findAny().isEmpty()) Files.delete(temporarios);
        }
        log.accept("%d UF(s) prontas em %s".formatted(faltando.size(), diretorio));
        marcarCompleto();
        return true;
    }

    private void marcarCompleto() throws IOException {
        Files.writeString(diretorio.resolve(MARCADOR), String.join("\n", new VotoReader(diretorio).ufs()) + "\n");
    }

    private static String tamanho(long bytes) {
        if (bytes < 0) return "tamanho desconhecido";
        if (bytes >= 1_000_000_000) return String.format(PT_BR, "%.1f GB", bytes / 1e9);
        return String.format(PT_BR, "%.1f MB", bytes / 1e6);
    }
}
