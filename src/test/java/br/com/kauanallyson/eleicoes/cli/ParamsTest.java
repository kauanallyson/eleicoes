/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.cli;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;
import br.com.kauanallyson.eleicoes.relatorio.TipoDeRelatorio;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParamsTest {

    private static final Set<TipoDeRelatorio> TODOS = EnumSet.allOf(TipoDeRelatorio.class);

    @Test
    void semArgumentosEhO2oTurnoDe2026ComTodosOsRelatoriosSemAutorizarDownload() {
        assertEquals(new Params(new Eleicao(2026, 2), false, TODOS), Params.parse(new String[0]));
    }

    @Test
    void anoTurnoESim() {
        assertEquals(new Params(new Eleicao(2022, 1), true, TODOS), Params.parse(new String[]{"--turno=1", "--sim", "--ano=2022"}));
    }

    @Test
    void relatoriosEscolhidosPorNomeIgnorandoEspacosERepeticoes() {
        var params = Params.parse(new String[]{"--relatorios=totais, por-estado,totais"});

        assertEquals(Set.of(TipoDeRelatorio.TOTAIS, TipoDeRelatorio.POR_ESTADO), params.relatorios());
    }

    @Test
    void relatorioDesconhecidoListaOsDisponiveis() {
        var erro = assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--relatorios=totais,mapa"}));

        assertTrue(erro.getMessage().contains("relatório desconhecido: mapa"), erro.getMessage());
        assertTrue(erro.getMessage().contains("por-estado"), erro.getMessage());
    }

    @Test
    void argumentoDesconhecidoOuInvalidoEhErro() {
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--ano"}));
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--ano=dois mil"}));
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--turno=3"}));
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--relatorios="}));
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--relatorios=,"}));
    }
}
