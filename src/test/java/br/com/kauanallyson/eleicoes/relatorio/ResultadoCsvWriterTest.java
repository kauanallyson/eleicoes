/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResultadoCsvWriterTest {

    @Test
    void formataNoPadraoDoTseEApagaOsCsvsAnteriores(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("antigo_1715.csv"), "x");
        Files.writeString(dir.resolve("anotacoes.txt"), "fica");

        new ResultadoCsvWriter(dir).gravar(List.of(new Tabela("t.csv", List.of("NOME", "VOTOS", "PERCENTUAL", "OK"),
                List.of(List.of("LULA", 64256232L, 50.035, true)))));

        assertFalse(Files.exists(dir.resolve("antigo_1715.csv")));
        assertEquals("fica", Files.readString(dir.resolve("anotacoes.txt")));
        assertEquals("\"NOME\";\"VOTOS\";\"PERCENTUAL\";\"OK\"\n\"LULA\";\"64256232\";\"50,04\";\"SIM\"\n",
                Files.readString(dir.resolve("t.csv")));
    }
}
