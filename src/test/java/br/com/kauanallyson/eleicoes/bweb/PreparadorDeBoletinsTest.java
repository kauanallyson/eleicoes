/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreparadorDeBoletinsTest {

    private static final String CSV = "\"NM_TIPO_ELEICAO\";\"NM_MUNICIPIO\"\r\n\"Eleição Ordinária\";\"SÃO PAULO\"\r\n";

    @Test
    void extraiOCsvEConverteDeLatin1ParaUtf8(@TempDir Path dir) throws Exception {
        pacote(dir.resolve("bweb_2t_SP_261020260742.zip"), "bweb_2t_SP_261020260742.csv");
        Files.writeString(dir.resolve("SP.csv"), "versão antiga");

        var ufs = new PreparadorDeBoletins(dir).preparar();

        assertEquals(List.of("SP"), ufs);
        assertEquals(CSV, Files.readString(dir.resolve("SP.csv"), StandardCharsets.UTF_8));
        try (var arquivos = Files.list(dir)) {
            assertEquals(0, arquivos.filter(f -> f.toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void doisPacotesDaMesmaUfSaoErro(@TempDir Path dir) throws Exception {
        pacote(dir.resolve("bweb_1t_AC_051020260700.zip"), "bweb_1t_AC.csv");
        pacote(dir.resolve("bweb_2t_AC_261020260742.zip"), "bweb_2t_AC.csv");

        assertThrows(IllegalStateException.class, () -> new PreparadorDeBoletins(dir).preparar());
    }

    @Test
    void semPacoteNaoFazNada(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), "já preparado");

        assertEquals(List.of(), new PreparadorDeBoletins(dir).preparar());
        assertEquals("já preparado", Files.readString(dir.resolve("AC.csv")));
    }

    private static void pacote(Path zip, String nomeDoCsv) throws IOException {
        try (var saida = new ZipOutputStream(Files.newOutputStream(zip))) {
            saida.putNextEntry(new ZipEntry("_leiame-boletimurnaweb.pdf"));
            saida.write(new byte[]{'%', 'P', 'D', 'F'});
            saida.closeEntry();
            saida.putNextEntry(new ZipEntry(nomeDoCsv));
            saida.write(CSV.getBytes(StandardCharsets.ISO_8859_1));
            saida.closeEntry();
        }
    }
}
