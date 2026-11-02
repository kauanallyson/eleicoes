/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import br.com.kauanallyson.eleicoes.apuracao.VotosNaSecao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VotoReaderTest {

    @Test
    void ufEhONomeDoArquivoEOsRegistrosVemDoCsv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), """
                "SG_UF";"CD_MUNICIPIO";"NR_ZONA";"NR_SECAO";"DS_CARGO_PERGUNTA";"QT_APTOS";"QT_COMPARECIMENTO";"NM_VOTAVEL";"QT_VOTOS";"DT_ENCERRAMENTO"\r
                "AC";"1538";"8";"68";"Presidente";"220";"169";"LULA";"43";"2026-10-25 15:00:48"\r
                "AC";"1538";"8";"68";"Presidente";"220";"169";"Branco";"2";""\r
                """);
        Files.writeString(dir.resolve("leia-me.txt"), "não é boletim");

        var leitor = new VotoReader(dir);

        assertEquals(List.of("AC"), leitor.ufs());
        try (var registros = leitor.registros("AC")) {
            assertEquals(List.of(
                    new VotosNaSecao("AC", 1538, 8, 68, "Presidente", "LULA", 43, 220, 169, LocalDateTime.of(2026, 10, 25, 15, 0, 48)),
                    new VotosNaSecao("AC", 1538, 8, 68, "Presidente", "Branco", 2, 220, 169, null)), registros.toList());
        }
    }

    /** Em 2022 o TSE gravou as datas no formato brasileiro. */
    @Test
    void leOEncerramentoNoFormatoDe2022(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), """
                "SG_UF";"CD_MUNICIPIO";"NR_ZONA";"NR_SECAO";"DS_CARGO_PERGUNTA";"QT_APTOS";"QT_COMPARECIMENTO";"NM_VOTAVEL";"QT_VOTOS";"DT_ENCERRAMENTO"\r
                "AC";"1392";"9";"12";"Presidente";"300";"240";"Nulo";"1";"02/10/2022 15:01:09"\r
                """);

        try (var registros = new VotoReader(dir).registros("AC")) {
            assertEquals(LocalDateTime.of(2022, 10, 2, 15, 1, 9), registros.toList().getFirst().encerramentoLocal());
        }
    }

    /** Sem aptos, comparecimento, zona ou seção o relatório de comparecimento sairia zerado sem aviso. */
    @Test
    void recusaBoletimSemAsColunasDaSecao(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("AC.csv"), """
                "SG_UF";"CD_MUNICIPIO";"DS_CARGO_PERGUNTA";"NM_VOTAVEL";"QT_VOTOS";"DT_ENCERRAMENTO"\r
                "AC";"1538";"Presidente";"LULA";"43";"2026-10-25 15:00:48"\r
                """);

        var erro = assertThrows(IOException.class, () -> new VotoReader(dir).registros("AC"));

        for (var coluna : List.of("NR_ZONA", "NR_SECAO", "QT_APTOS", "QT_COMPARECIMENTO")) {
            assertTrue(erro.getMessage().contains(coluna), erro.getMessage());
        }
    }
}
