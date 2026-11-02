/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import br.com.kauanallyson.eleicoes.apuracao.VotosNaSecao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VotoReaderTest {

    @Test
    void ufEhONomeDoArquivoEOsRegistrosVemDoCsv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), """
                "SG_UF";"CD_MUNICIPIO";"DS_CARGO_PERGUNTA";"NM_VOTAVEL";"QT_VOTOS";"DT_ENCERRAMENTO"\r
                "AC";"1538";"Presidente";"LULA";"43";"2026-10-25 15:00:48"\r
                "AC";"1538";"Presidente";"Branco";"2";""\r
                """);
        Files.writeString(dir.resolve("leia-me.txt"), "não é boletim");

        var leitor = new VotoReader(dir);

        assertEquals(List.of("AC"), leitor.ufs());
        try (var registros = leitor.registros("AC")) {
            assertEquals(List.of(
                    new VotosNaSecao("AC", 1538, "Presidente", "LULA", 43, LocalDateTime.of(2026, 10, 25, 15, 0, 48)),
                    new VotosNaSecao("AC", 1538, "Presidente", "Branco", 2, null)), registros.toList());
        }
    }

    /** Em 2022 o TSE gravou as datas no formato brasileiro. */
    @Test
    void leOEncerramentoNoFormatoDe2022(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), """
                "SG_UF";"CD_MUNICIPIO";"DS_CARGO_PERGUNTA";"NM_VOTAVEL";"QT_VOTOS";"DT_ENCERRAMENTO"\r
                "AC";"1392";"Presidente";"Nulo";"1";"02/10/2022 15:01:09"\r
                """);

        try (var registros = new VotoReader(dir).registros("AC")) {
            assertEquals(LocalDateTime.of(2022, 10, 2, 15, 1, 9), registros.toList().getFirst().encerramentoLocal());
        }
    }
}
