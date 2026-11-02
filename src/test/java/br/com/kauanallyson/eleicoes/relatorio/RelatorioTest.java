/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelatorioTest {

    private static List<String> arquivos(Path dir) throws Exception {
        try (var lista = Files.list(dir)) {
            return lista.map(f -> f.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void semEscolherGravaTodos(@TempDir Path dir) throws Exception {
        Relatorio.builder().diretorio(dir).build().gravar(RespostasTest.boletins());

        assertEquals(List.of("candidatos_ate_e_apos_fim_da_votacao_1710.csv", "comparecimento_e_abstencao_por_estado.csv",
                "votos_depois_do_fim_da_votacao_1710.csv",
                "votos_por_estado.csv", "votos_totais.csv"), arquivos(dir));
    }

    @Test
    void gravaSoOsEscolhidos(@TempDir Path dir) throws Exception {
        Relatorio.builder().diretorio(dir).tipos(List.of(TipoDeRelatorio.TOTAIS)).build().gravar(RespostasTest.boletins());

        assertEquals(List.of("votos_totais.csv"), arquivos(dir));
    }

    @Test
    void logaOAndamentoEmPercentualPorCsv(@TempDir Path dir) throws Exception {
        var log = new ArrayList<String>();

        Relatorio.builder().diretorio(dir).tipos(List.of(TipoDeRelatorio.TOTAIS, TipoDeRelatorio.POR_ESTADO))
                .log(log::add).build().gravar(RespostasTest.boletins());

        assertEquals(List.of("[relatórios]  50% (1/2) votos_por_estado.csv", "[relatórios] 100% (2/2) votos_totais.csv"), log);
    }

    @Test
    void exigeAoMenosUm() {
        assertThrows(IllegalArgumentException.class, () -> Relatorio.builder().tipos(List.of()));
    }
}
