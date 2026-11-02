/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O adapter HTTP contra um TSE simulado: API de catálogo, pacotes .zip (com HEAD e Range) e .sha512. */
class PortalTseHttpTest {

    private static final byte[] ZIP_AC = "conteúdo do pacote do AC".repeat(100).getBytes(StandardCharsets.UTF_8);

    private final Map<String, byte[]> arquivos = new ConcurrentHashMap<>();
    private final List<String> pedidos = new CopyOnWriteArrayList<>();
    private HttpServer servidor;
    private PortalTseHttp portal;

    @BeforeEach
    void iniciarTse() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api", this::catalogo);
        servidor.createContext("/buweb/", this::arquivo);
        servidor.start();
        publicar("bweb_2t_AC_261020260742.zip", ZIP_AC);
        publicar("bweb_2t_SP_261020260742.zip", new byte[]{1, 2, 3});
        var api = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/api");
        portal = new PortalTseHttp(HttpClient.newHttpClient(), api, 2, mensagem -> { });
    }

    @AfterEach
    void pararTse() {
        servidor.stop(0);
    }

    @Test
    void listaSoOsPacotesDoTurnoPeloCatalogo() throws Exception {
        var pacotes = portal.pacotes();

        assertEquals(List.of("AC", "SP"), pacotes.stream().map(PacoteTse::uf).toList());
        assertEquals("bweb_2t_AC_261020260742.zip", pacotes.getFirst().nomeDoArquivo());
        assertTrue(pacotes.getFirst().sha512().toString().endsWith("bweb_2t_AC_261020260742.zip.sha512"));
        assertEquals(PacoteTse.TAMANHO_DESCONHECIDO, pacotes.getFirst().tamanho());
    }

    @Test
    void tamanhoPeloHead() throws Exception {
        var ac = portal.comTamanho(portal.pacotes().getFirst());

        assertEquals(ZIP_AC.length, ac.tamanho());
        assertTrue(pedidos.contains("HEAD /buweb/bweb_2t_AC_261020260742.zip"));
    }

    @Test
    void baixaEConfereOHash(@TempDir Path dir) throws Exception {
        var destino = dir.resolve("AC.zip");

        portal.baixar(portal.comTamanho(portal.pacotes().getFirst()), destino);

        assertArrayEquals(ZIP_AC, Files.readAllBytes(destino));
        assertTrue(pedidos.contains("GET /buweb/bweb_2t_AC_261020260742.zip.sha512"));
    }

    @Test
    void retomaUmDownloadInterrompido(@TempDir Path dir) throws Exception {
        var destino = dir.resolve("AC.zip");
        Files.write(destino, Arrays.copyOf(ZIP_AC, ZIP_AC.length / 2));

        portal.baixar(portal.comTamanho(portal.pacotes().getFirst()), destino);

        assertTrue(pedidos.contains("GET /buweb/bweb_2t_AC_261020260742.zip Range=bytes=" + ZIP_AC.length / 2 + "-"), pedidos::toString);
        assertArrayEquals(ZIP_AC, Files.readAllBytes(destino));
    }

    @Test
    void hashQueNaoConfereApagaOArquivoEEhErro(@TempDir Path dir) throws Exception {
        arquivos.put("bweb_2t_AC_261020260742.zip.sha512", ("0".repeat(128) + "  bweb_2t_AC_261020260742.zip").getBytes());
        var destino = dir.resolve("AC.zip");

        assertThrows(IOException.class, () -> portal.baixar(portal.pacotes().getFirst(), destino));
        assertFalse(Files.exists(destino));
    }

    // ---- TSE simulado ----

    private void publicar(String nome, byte[] zip) throws Exception {
        arquivos.put(nome, zip);
        var hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(zip));
        arquivos.put(nome + ".sha512", (hash + "  " + nome + "\n").getBytes(StandardCharsets.US_ASCII));
    }

    private void catalogo(HttpExchange troca) throws IOException {
        var base = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/buweb/";
        var recursos = new StringBuilder("{\"success\": true, \"result\": {\"resources\": [");
        recursos.append("{\"name\": \"AC - Primeiro turno\", \"url\": \"").append(base).append("bweb_1t_AC_051020261403.zip\"},");
        for (var nome : List.of("bweb_2t_SP_261020260742.zip", "bweb_2t_AC_261020260742.zip")) {
            recursos.append("{\"url\": \"").append(base).append(nome).append("\"},");
            recursos.append("{\"url\": \"").append(base).append(nome).append(".sha512\"},");
        }
        recursos.setLength(recursos.length() - 1);
        recursos.append("]}}");
        responder(troca, 200, recursos.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void arquivo(HttpExchange troca) throws IOException {
        var nome = troca.getRequestURI().getPath().substring("/buweb/".length());
        var range = troca.getRequestHeaders().getFirst("Range");
        pedidos.add(troca.getRequestMethod() + " /buweb/" + nome + (range == null ? "" : " Range=" + range));
        var conteudo = arquivos.get(nome);
        if (conteudo == null) {
            responder(troca, 404, new byte[0]);
        } else if (troca.getRequestMethod().equals("HEAD")) {
            troca.getResponseHeaders().set("Content-Length", String.valueOf(conteudo.length));
            troca.sendResponseHeaders(200, -1);
            troca.close();
        } else if (range != null) {
            int inicio = Integer.parseInt(range.replaceAll("bytes=(\\d+)-", "$1"));
            responder(troca, 206, Arrays.copyOfRange(conteudo, inicio, conteudo.length));
        } else {
            responder(troca, 200, conteudo);
        }
    }

    private static void responder(HttpExchange troca, int status, byte[] corpo) throws IOException {
        troca.sendResponseHeaders(status, corpo.length == 0 ? -1 : corpo.length);
        try (var saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }
}
