/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoDeRelatorioTest {

    @Test
    void cadaTipoGeraOSeuArquivo() {
        var arquivos = Arrays.stream(TipoDeRelatorio.values()).map(t -> t.gerar(RespostasTest.boletins()).nome()).toList();

        assertEquals(List.of("votos_por_estado.csv", "votos_totais.csv", "votos_depois_do_fim_da_votacao_1710.csv",
                "candidatos_ate_e_apos_fim_da_votacao_1710.csv", "comparecimento_e_abstencao_por_estado.csv"), arquivos);
    }

    @Test
    void achaPeloNomeDaLinhaDeComando() {
        for (var tipo : TipoDeRelatorio.values()) {
            assertEquals(tipo, TipoDeRelatorio.porNome(tipo.nome()));
        }
    }

    @Test
    void nomeDesconhecidoMostraOCatalogo() {
        var erro = assertThrows(IllegalArgumentException.class, () -> TipoDeRelatorio.porNome("mapa"));

        for (var tipo : TipoDeRelatorio.values()) {
            assertTrue(erro.getMessage().contains(tipo.nome() + " "), erro.getMessage());
            assertTrue(erro.getMessage().contains(tipo.descricao()), erro.getMessage());
        }
    }
}
