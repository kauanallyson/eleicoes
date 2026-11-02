/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Adapter HTTP do {@link PortalTse}: descobre os pacotes pela API do Portal de Dados Abertos (CKAN), em vez de
 * deixar as URLs fixas no código; o tamanho vem de um HEAD, porque a API não informa. O download retoma de onde
 * parou (Range) se a conexão cair.
 */
public class PortalTseHttp implements PortalTse {

    private static final int TENTATIVAS = 5;

    private final HttpClient http;
    private final URI api;
    private final int turno;
    private final Consumer<String> log;

    public PortalTseHttp(HttpClient http, URI api, int turno, Consumer<String> log) {
        this.http = http;
        this.api = api;
        this.turno = turno;
        this.log = mensagem -> log.accept("[dados] " + mensagem);
    }

    /** Conjunto "Resultados &lt;ano&gt; - Boletim de Urna" do Portal de Dados Abertos do TSE. */
    public static PortalTseHttp boletins(HttpClient http, Eleicao eleicao, Consumer<String> log) {
        var api = URI.create("https://dadosabertos.tse.jus.br/api/3/action/package_show?id=resultados-"
                + eleicao.ano() + "-boletim-de-urna");
        return new PortalTseHttp(http, api, eleicao.turno(), log);
    }

    /** Em 2018 os pacotes se chamam {@code BWEB_...}; nos outros anos, {@code bweb_...}. */
    @Override
    public List<PacoteTse> pacotes() throws IOException, InterruptedException {
        var resposta = http.send(HttpRequest.newBuilder(api).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() != 200) {
            throw new IOException("Catálogo do TSE respondeu " + resposta.statusCode() + ": " + api);
        }

        var url = Pattern.compile("\"url\"\\s*:\\s*\"(https?://[^\"]+/(?i:bweb)_" + turno + "t_([A-Z]{2})_\\d+\\.zip)\"");
        var pacotes = new ArrayList<PacoteTse>();
        var m = url.matcher(resposta.body());
        while (m.find()) {
            pacotes.add(new PacoteTse(m.group(2), URI.create(m.group(1)), URI.create(m.group(1) + ".sha512"),
                    PacoteTse.TAMANHO_DESCONHECIDO));
        }
        if (pacotes.isEmpty()) throw new IOException("Nenhum pacote do " + turno + "º turno no catálogo do TSE: " + api);
        pacotes.sort(Comparator.comparing(PacoteTse::uf));
        return pacotes;
    }

    @Override
    public PacoteTse comTamanho(PacoteTse pacote) throws IOException, InterruptedException {
        var head = HttpRequest.newBuilder(pacote.zip()).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        return pacote.comTamanho(http.send(head, HttpResponse.BodyHandlers.discarding()).headers()
                .firstValueAsLong("content-length").orElse(PacoteTse.TAMANHO_DESCONHECIDO));
    }

    @Override
    public void baixar(PacoteTse pacote, Path destino) throws IOException, InterruptedException {
        transferir(pacote, destino);
        conferirHash(pacote, destino);
    }

    private void transferir(PacoteTse pacote, Path destino) throws IOException, InterruptedException {
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
        if (!sha512(zip).equals(esperado)) {
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
}
