/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.Candidato;
import br.com.kauanallyson.eleicoes.apuracao.Contagem;
import br.com.kauanallyson.eleicoes.apuracao.HorarioDaVotacao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoEstado;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RespostasTest {

    private static final HorarioDaVotacao HORARIO = new HorarioDaVotacao(LocalDate.of(2026, 10, 25), Duration.ofMinutes(10));

    private static ResultadoEstado estado(String uf, Map<String, Long> votos, Map<String, Long> ate, Map<String, Long> depois) {
        var porCandidato = new HashMap<Candidato, Long>();
        votos.forEach((nome, qt) -> porCandidato.put(new Candidato("Presidente", nome), qt));
        return new ResultadoEstado(uf, Contagem.de(porCandidato), Contagem.de(ate), Contagem.de(depois));
    }

    private static final ResultadoApuracao BOLETINS = new ResultadoApuracao(List.of(
            estado("AC", Map.of("FLAVIO BOLSONARO", 30L, "LULA", 10L), Map.of("FLAVIO BOLSONARO", 30L, "LULA", 9L), Map.of("LULA", 1L)),
            estado("PE", Map.of("LULA", 60L, "FLAVIO BOLSONARO", 20L), Map.of("LULA", 60L, "FLAVIO BOLSONARO", 17L), Map.of("FLAVIO BOLSONARO", 3L))),
            HORARIO);

    private static Tabela tabela(List<Tabela> tabelas, String nome) {
        return tabelas.stream().filter(t -> t.nome().equals(nome)).findFirst().orElseThrow();
    }

    @Test
    void umaTabelaPorResposta() {
        var nomes = Respostas.de(BOLETINS).stream().map(Tabela::nome).toList();

        assertEquals(List.of("votos_por_estado.csv", "votos_totais.csv", "votos_depois_do_fim_da_votacao_1710.csv",
                "candidatos_ate_e_apos_fim_da_votacao_1710.csv"), nomes);
    }

    @Test
    void votosPorEstadoComPercentualDaUf() {
        var linhas = tabela(Respostas.de(BOLETINS), "votos_por_estado.csv").linhas();

        assertEquals(List.of("AC", "FLAVIO BOLSONARO", 30L, 75.0), linhas.get(0));
        assertEquals(List.of("AC", "LULA", 10L, 25.0), linhas.get(1));
    }

    @Test
    void votosDepoisDoFimOrdenadoPorPercentualComTotalDoPais() {
        var linhas = tabela(Respostas.de(BOLETINS), "votos_depois_do_fim_da_votacao_1710.csv").linhas();

        assertEquals(List.of(
                List.of("PE", 3L, 3.75),
                List.of("AC", 1L, 2.5),
                List.of("BR", 4L, (double) 100 * 4 / 120)), linhas);
    }

    @Test
    void candidatosComPercentualDentroDeCadaGrupoDeSecoes() {
        var linhas = tabela(Respostas.de(BOLETINS), "candidatos_ate_e_apos_fim_da_votacao_1710.csv").linhas();

        assertEquals(List.of("LULA", 69L, 100.0 * 69 / 116, 1L, 25.0), linhas.get(0));
        assertEquals(List.of("TOTAL", 116L, 100.0, 4L, 100.0), linhas.getLast());
    }

}
