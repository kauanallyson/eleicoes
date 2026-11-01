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

import java.io.ByteArrayOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Simula o TSE com um servidor HTTP local: API de catálogo, pacotes .zip (com HEAD e Range) e .sha512. */
class DownloadDeBoletinsTest {

    private static final String CSV_AC = "\"NM_TIPO_ELEICAO\";\"NM_MUNICIPIO\"\r\n\"Eleição Ordinária\";\"SENADOR GUIOMARD\"\r\n";
    private static final String CSV_SP = "\"NM_TIPO_ELEICAO\";\"NM_MUNICIPIO\"\r\n\"Eleição Ordinária\";\"SÃO PAULO\"\r\n";

    private final Map<String, byte[]> arquivos = new ConcurrentHashMap<>();
    private final List<String> pedidos = new CopyOnWriteArrayList<>();
    private final List<String> log = new CopyOnWriteArrayList<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private HttpServer servidor;

    @BeforeEach
    void iniciarTse() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api", this::catalogo);
        servidor.createContext("/buweb/", this::arquivo);
        servidor.start();
        publicar("bweb_2t_AC_261020260742.zip", pacote("bweb_2t_AC_261020260742.csv", CSV_AC));
        publicar("bweb_2t_SP_261020260742.zip", pacote("bweb_2t_SP_261020260742.csv", CSV_SP));
    }

    @AfterEach
    void pararTse() {
        servidor.stop(0);
    }

    private DownloadDeBoletins download(Path dir, Confirmacao confirmacao) {
        var api = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/api");
        return new DownloadDeBoletins(dir, new CatalogoTse(http, api, 2), confirmacao, http, log::add);
    }

    @Test
    void baixaSoAsUfsQueFaltamConfereOHashEConverteParaUtf8(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("SP.csv"), "já preparado");

        assertTrue(download(dir, pergunta -> true).garantir());

        assertEquals(CSV_AC, Files.readString(dir.resolve("AC.csv"), StandardCharsets.UTF_8));
        assertEquals("já preparado", Files.readString(dir.resolve("SP.csv")));
        assertFalse(Files.exists(dir.resolve(".download")));
        assertFalse(pedidos.contains("GET /buweb/bweb_2t_SP_261020260742.zip"));
        assertTrue(pedidos.contains("GET /buweb/bweb_2t_AC_261020260742.zip.sha512"));
    }

    @Test
    void avisaQuaisUfsFaltamEOTamanhoAntesDePerguntar(@TempDir Path dir) throws Exception {
        download(dir, pergunta -> {
            assertTrue(log.stream().anyMatch(l -> l.contains("2 UF(s): AC, SP")), log::toString);
            assertTrue(log.stream().anyMatch(l -> l.startsWith("[dados] download: ") && l.contains("MB (2 arquivos .zip)")), log::toString);
            return false;
        }).garantir();
    }

    @Test
    void semConfirmacaoNaoBaixaNada(@TempDir Path dir) throws Exception {
        assertFalse(download(dir, pergunta -> false).garantir());

        try (var conteudo = Files.list(dir)) {
            assertEquals(0, conteudo.count());
        }
        assertTrue(pedidos.stream().noneMatch(p -> p.startsWith("GET /buweb/")), pedidos::toString);
    }

    @Test
    void comTudoPresenteNaoPerguntaNemBaixa(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), "x");
        Files.writeString(dir.resolve("SP.csv"), "x");

        assertTrue(download(dir, pergunta -> fail("não deveria perguntar")).garantir());
        assertTrue(pedidos.stream().noneMatch(p -> p.startsWith("GET /buweb/")), pedidos::toString);
    }

    @Test
    void hashQueNaoConfereEhErroENaoDeixaCsv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("SP.csv"), "x");
        arquivos.put("bweb_2t_AC_261020260742.zip.sha512", ("0".repeat(128) + "  bweb_2t_AC_261020260742.zip").getBytes());

        assertThrows(IOException.class, () -> download(dir, pergunta -> true).garantir());
        assertFalse(Files.exists(dir.resolve("AC.csv")));
        assertFalse(Files.exists(dir.resolve(".download").resolve("bweb_2t_AC_261020260742.zip")));
    }

    @Test
    void retomaUmDownloadInterrompido(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("SP.csv"), "x");
        var zip = arquivos.get("bweb_2t_AC_261020260742.zip");
        var parcial = Files.createDirectories(dir.resolve(".download")).resolve("bweb_2t_AC_261020260742.zip");
        Files.write(parcial, Arrays.copyOf(zip, zip.length / 2));

        assertTrue(download(dir, pergunta -> true).garantir());

        assertTrue(pedidos.contains("GET /buweb/bweb_2t_AC_261020260742.zip Range=bytes=" + zip.length / 2 + "-"), pedidos::toString);
        assertEquals(CSV_AC, Files.readString(dir.resolve("AC.csv"), StandardCharsets.UTF_8));
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
        for (var nome : List.of("bweb_2t_AC_261020260742.zip", "bweb_2t_SP_261020260742.zip")) {
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

    private static byte[] pacote(String nomeDoCsv, String csv) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("_leiame-boletimurnaweb.pdf"));
            zip.write(new byte[]{'%', 'P', 'D', 'F'});
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(nomeDoCsv));
            zip.write(csv.getBytes(StandardCharsets.ISO_8859_1));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
