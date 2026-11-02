/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** A política do download (o que falta, confirmação, conversão, marcador) com um {@link PortalTse} em memória. */
class DownloadDeBoletinsTest {

    private static final String CSV_AC = "\"NM_TIPO_ELEICAO\";\"NM_MUNICIPIO\"\r\n\"Eleição Ordinária\";\"SENADOR GUIOMARD\"\r\n";
    private static final String CSV_SP = "\"NM_TIPO_ELEICAO\";\"NM_MUNICIPIO\"\r\n\"Eleição Ordinária\";\"SÃO PAULO\"\r\n";

    private final PortalEmMemoria portal = new PortalEmMemoria(Map.of("AC", pacote(CSV_AC), "SP", pacote(CSV_SP)));
    private final List<String> log = new CopyOnWriteArrayList<>();

    private DownloadDeBoletins download(Path dir, Confirmation confirmation) {
        return new DownloadDeBoletins(dir, portal, confirmation, log::add);
    }

    @Test
    void baixaSoAsUfsQueFaltamEConverteParaUtf8(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("SP.csv"), "já preparado");

        assertTrue(download(dir, question -> true).garantir());

        assertEquals(CSV_AC, Files.readString(dir.resolve("AC.csv"), StandardCharsets.UTF_8));
        assertEquals("já preparado", Files.readString(dir.resolve("SP.csv")));
        assertFalse(Files.exists(dir.resolve(".download")));
        assertEquals(List.of("pacotes", "tamanho AC", "baixar AC"), portal.chamadas, "só consulta e baixa o que falta");
    }

    @Test
    void avisaQuaisUfsFaltamEOTamanhoAntesDePerguntar(@TempDir Path dir) throws Exception {
        download(dir, question -> {
            assertTrue(log.stream().anyMatch(l -> l.contains("2 UF(s): AC, SP")), log::toString);
            assertTrue(log.stream().anyMatch(l -> l.startsWith("[dados] download: ") && l.contains("(2 arquivos .zip)")), log::toString);
            return false;
        }).garantir();
    }

    @Test
    void semConfirmacaoNaoBaixaNada(@TempDir Path dir) throws Exception {
        assertFalse(download(dir, question -> false).garantir());

        try (var conteudo = Files.list(dir)) {
            assertEquals(0, conteudo.count());
        }
        assertTrue(portal.chamadas.stream().noneMatch(c -> c.startsWith("baixar")), portal.chamadas::toString);
    }

    @Test
    void comTudoPresenteNaoPerguntaNemBaixa(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), "x");
        Files.writeString(dir.resolve("SP.csv"), "x");

        assertTrue(download(dir, question -> fail("não deveria perguntar")).garantir());
        assertEquals(List.of("pacotes"), portal.chamadas);
    }

    @Test
    void depoisDeCompletoNaoConsultaMaisOTse(@TempDir Path dir) throws Exception {
        assertTrue(download(dir, question -> true).garantir());
        portal.chamadas.clear();

        assertTrue(download(dir, question -> fail("não deveria perguntar")).garantir());
        assertEquals(List.of(), portal.chamadas);
    }

    @Test
    void baixaAsUfsAoMesmoTempo(@TempDir Path dir) throws Exception {
        portal.todasBaixando = new CountDownLatch(2);

        assertTrue(download(dir, question -> true).garantir());

        assertTrue(Files.exists(dir.resolve("AC.csv")) && Files.exists(dir.resolve("SP.csv")));
    }

    @Test
    void falhaDeUmaUfCancelaAsOutrasSemEsperarElasTerminarem(@TempDir Path dir) {
        portal.ufLenta = "AC";
        portal.ufQueFalha = "SP";

        long inicio = System.nanoTime();
        var erro = assertThrows(IOException.class, () -> download(dir, question -> true).garantir());

        assertEquals("SP falhou", erro.getMessage());
        assertTrue(Duration.ofNanos(System.nanoTime() - inicio).toSeconds() < 5, "esperou a UF lenta terminar");
        assertFalse(Files.exists(dir.resolve("AC.csv")));
    }

    @Test
    void erroNoDownloadNaoDeixaCsv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("SP.csv"), "x");
        portal.falharAoBaixar = true;

        assertThrows(IOException.class, () -> download(dir, question -> true).garantir());
        assertFalse(Files.exists(dir.resolve("AC.csv")));
    }

    private static final class PortalEmMemoria implements PortalTse {

        private final Map<String, byte[]> zips;
        private final List<String> chamadas = new CopyOnWriteArrayList<>();
        private volatile boolean falharAoBaixar;
        /** Se presente, cada download só termina depois que todos começaram: em série, nunca terminaria. */
        private volatile CountDownLatch todasBaixando;
        /** Demora 30 s (interrompível) para baixar. */
        private volatile String ufLenta;
        /** Falha na hora. */
        private volatile String ufQueFalha;

        private PortalEmMemoria(Map<String, byte[]> zips) {
            this.zips = zips;
        }

        @Override
        public List<PacoteTse> pacotes() {
            chamadas.add("pacotes");
            return zips.keySet().stream().sorted()
                    .map(uf -> new PacoteTse(uf, URI.create("mem:/bweb_2t_" + uf + "_261020260742.zip"),
                            URI.create("mem:/bweb_2t_" + uf + "_261020260742.zip.sha512"), PacoteTse.TAMANHO_DESCONHECIDO))
                    .toList();
        }

        @Override
        public PacoteTse comTamanho(PacoteTse pacote) {
            chamadas.add("tamanho " + pacote.uf());
            return pacote.comTamanho(zips.get(pacote.uf()).length);
        }

        @Override
        public void baixar(PacoteTse pacote, Path destino) throws IOException {
            chamadas.add("baixar " + pacote.uf());
            if (falharAoBaixar) throw new IOException("SHA-512 não confere");
            if (pacote.uf().equals(ufQueFalha)) throw new IOException(pacote.uf() + " falhou");
            if (pacote.uf().equals(ufLenta)) {
                try {
                    Thread.sleep(Duration.ofSeconds(30));
                } catch (InterruptedException e) {
                    throw new IOException("cancelada", e);
                }
            }
            if (todasBaixando != null) {
                todasBaixando.countDown();
                try {
                    if (!todasBaixando.await(5, TimeUnit.SECONDS)) throw new IOException("as UFs não foram baixadas ao mesmo tempo");
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
            Files.write(destino, zips.get(pacote.uf()));
        }
    }

    private static byte[] pacote(String csv) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("_leiame-boletimurnaweb.pdf"));
                zip.write(new byte[]{'%', 'P', 'D', 'F'});
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("bweb.csv"));
                zip.write(csv.getBytes(StandardCharsets.ISO_8859_1));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
