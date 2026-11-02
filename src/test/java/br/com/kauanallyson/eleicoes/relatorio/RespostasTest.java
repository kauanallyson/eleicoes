/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.Comparecimento;
import br.com.kauanallyson.eleicoes.apuracao.Contagem;
import br.com.kauanallyson.eleicoes.apuracao.HorarioDaVotacao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoEstado;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RespostasTest {

    private static final HorarioDaVotacao HORARIO = new HorarioDaVotacao(LocalDate.of(2026, 10, 25), Duration.ofMinutes(10));

    private static ResultadoEstado estado(String uf, Map<String, Long> votos, Map<String, Long> ate, Map<String, Long> depois,
                                          Comparecimento comparecimento) {
        return new ResultadoEstado(uf, Contagem.de(votos), Contagem.de(ate), Contagem.de(depois), comparecimento);
    }

    private static final ResultadoApuracao BOLETINS = new ResultadoApuracao(List.of(
            estado("AC", Map.of("FLAVIO BOLSONARO", 30L, "LULA", 10L), Map.of("FLAVIO BOLSONARO", 30L, "LULA", 9L), Map.of("LULA", 1L),
                    new Comparecimento(50, 40)),
            estado("PE", Map.of("LULA", 60L, "FLAVIO BOLSONARO", 20L), Map.of("LULA", 60L, "FLAVIO BOLSONARO", 17L), Map.of("FLAVIO BOLSONARO", 3L),
                    new Comparecimento(100, 80))),
            HORARIO);

    /** O pacote reusa o mesmo resultado nos testes de {@link TipoDeRelatorio} e {@link Relatorio}. */
    static ResultadoApuracao boletins() {
        return BOLETINS;
    }

    @Test
    void votosPorEstadoComPercentualDaUf() {
        var linhas = Respostas.votosPorEstado(BOLETINS).linhas();

        assertEquals(List.of("AC", "FLAVIO BOLSONARO", 30L, new Percentual(75.0)), linhas.get(0));
        assertEquals(List.of("AC", "LULA", 10L, new Percentual(25.0)), linhas.get(1));
    }

    @Test
    void votosDepoisDoFimOrdenadoPorPercentualComTotalDoPais() {
        var linhas = Respostas.votosDepoisDoFimDaVotacao(BOLETINS).linhas();

        assertEquals(List.of(
                List.of("PE", 3L, new Percentual(3.75)),
                List.of("AC", 1L, new Percentual(2.5)),
                List.of("BR", 4L, Percentual.de(4, 120))), linhas);
    }

    @Test
    void candidatosComPercentualDentroDeCadaGrupoDeSecoes() {
        var linhas = Respostas.candidatosAteEDepoisDoFimDaVotacao(BOLETINS).linhas();

        assertEquals(List.of("LULA", 69L, Percentual.de(69, 116), 1L, new Percentual(25.0)), linhas.get(0));
        assertEquals(List.of("TOTAL", 116L, Percentual.CEM, 4L, Percentual.CEM), linhas.getLast());
    }

    @Test
    void comparecimentoOrdenadoPelaAbstencaoComTotalDoPais() {
        var boletins = new ResultadoApuracao(List.of(
                estado("AC", Map.of(), Map.of(), Map.of(), new Comparecimento(100, 90)),
                estado("ZZ", Map.of(), Map.of(), Map.of(), new Comparecimento(100, 40)),
                estado("SP", Map.of(), Map.of(), Map.of(), new Comparecimento(200, 150))),
                HORARIO);

        assertEquals(List.of(
                List.of("ZZ", 100L, 40L, new Percentual(40.0), 60L, new Percentual(60.0)),
                List.of("SP", 200L, 150L, new Percentual(75.0), 50L, new Percentual(25.0)),
                List.of("AC", 100L, 90L, new Percentual(90.0), 10L, new Percentual(10.0)),
                List.of("BR", 400L, 280L, new Percentual(70.0), 120L, new Percentual(30.0))),
                Respostas.comparecimentoPorEstado(boletins).linhas());
    }
}
