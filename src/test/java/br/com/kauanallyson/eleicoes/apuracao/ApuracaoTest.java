/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApuracaoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 25);

    private static VotosNaSecao presidente(String uf, String votavel, long votos, LocalDateTime encerramento) {
        return new VotosNaSecao(uf, 1, "Presidente", votavel, votos, encerramento);
    }

    private static ResultadoApuracao apurar(Map<String, List<VotosNaSecao>> porUf) throws Exception {
        return Apuracao.builder().boletins(new FonteEmMemoria<>(porUf)).eleicao(new Eleicao(2026, 2)).tolerancia(Duration.ofMinutes(10)).build().apurar();
    }

    @Test
    void soApuraPresidente() throws Exception {
        var resultado = apurar(Map.of("SP", List.of(
                presidente("SP", "A", 10, DIA.atTime(17, 0)),
                new VotosNaSecao("SP", 1, "Governador", "G", 99, DIA.atTime(17, 0)),
                new VotosNaSecao("SP", 1, "#NULO#", "#NULO#", 0, null))));

        assertEquals(Contagem.de(Map.of("A", 10L)), resultado.estados().getFirst().votos());
    }

    @Test
    void separaAsSecoesEncerradasAntesEDepoisDoLimite() throws Exception {
        var estado = apurar(Map.of("SP", List.of(
                presidente("SP", "A", 10, DIA.atTime(17, 10)),
                presidente("SP", "A", 3, DIA.atTime(17, 11)),
                presidente("SP", "B", 5, DIA.atTime(16, 0))))).estados().getFirst();

        assertEquals(Contagem.de(Map.of("A", 10L, "B", 5L)), estado.votosAteFimDaVotacao());
        assertEquals(Contagem.de(Map.of("A", 3L)), estado.votosDepoisDoFimDaVotacao());
    }

    @Test
    void secaoSemHorarioDeEncerramentoNaoEntraNaSeparacaoPorHorario() throws Exception {
        var estado = apurar(Map.of("ZZ", List.of(presidente("ZZ", "A", 7, null)))).estados().getFirst();

        assertEquals(7, estado.votos().total());
        assertEquals(0, estado.votosAteFimDaVotacao().total() + estado.votosDepoisDoFimDaVotacao().total());
    }

    @Test
    void umResultadoPorUfEmOrdemESomaDoPais() throws Exception {
        var resultado = apurar(Map.of(
                "SP", List.of(presidente("SP", "A", 10, DIA.atTime(17, 0))),
                "AC", List.of(presidente("AC", "A", 2, DIA.atTime(15, 0)), presidente("AC", "B", 4, DIA.atTime(15, 0)))));

        assertEquals(List.of("AC", "SP"), resultado.estados().stream().map(ResultadoEstado::uf).toList());
        assertEquals(Contagem.de(Map.of("A", 12L, "B", 4L)),
                resultado.somarEstados(ResultadoEstado::votos));
    }

    @Test
    void exigeUmaFonte() {
        assertThrows(NullPointerException.class, () -> Apuracao.builder().build());
    }
}
