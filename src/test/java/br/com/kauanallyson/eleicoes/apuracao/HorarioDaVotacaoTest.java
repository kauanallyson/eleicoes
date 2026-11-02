/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorarioDaVotacaoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 25);
    private final HorarioDaVotacao horario = new HorarioDaVotacao(DIA, Duration.ofMinutes(10));

    private static LocalDateTime as(int hora, int minuto) {
        return DIA.atTime(hora, minuto);
    }

    @Test
    void limiteEhOFimMaisATolerancia() {
        assertEquals(LocalTime.of(17, 10), horario.limite());
    }

    @Test
    void encerramentoEmBrasiliaComparaDiretoComOLimite() {
        assertFalse(horario.encerrouDepoisDoLimite("SP", 71072, as(17, 10)));
        assertTrue(horario.encerrouDepoisDoLimite("SP", 71072, as(17, 11)));
    }

    @Test
    void acreEstaDuasHorasAtrasDeBrasilia() {
        assertFalse(horario.encerrouDepoisDoLimite("AC", 1392, as(15, 5)));
        assertTrue(horario.encerrouDepoisDoLimite("AC", 1392, as(15, 11)));
    }

    @Test
    void oesteDoAmazonasEstaUmaHoraAtrasDeManaus() {
        int tabatinga = 98477, manaus = 2550;
        assertTrue(horario.encerrouDepoisDoLimite("AM", tabatinga, as(15, 30)));
        assertFalse(horario.encerrouDepoisDoLimite("AM", manaus, as(15, 30)));
    }

    @Test
    void fernandoDeNoronhaEstaUmaHoraAFrente() {
        assertFalse(horario.encerrouDepoisDoLimite("PE", 30015, as(18, 10)));
        assertTrue(horario.encerrouDepoisDoLimite("PE", 30015, as(18, 11)));
    }

    @Test
    void exteriorMantemOHorarioLocal() {
        assertTrue(horario.encerrouDepoisDoLimite("ZZ", 29539, as(17, 11)));
        assertFalse(horario.encerrouDepoisDoLimite("ZZ", 29539, as(15, 11)));
    }

    @Test
    void antesDe2022CadaSecaoEncerravaAs17hDoSeuFusoLocal() {
        var horario2018 = new HorarioDaVotacao(LocalDate.of(2018, 10, 28), Duration.ofMinutes(10));

        assertFalse(horario2018.encerrouDepoisDoLimite("AC", 1392, LocalDate.of(2018, 10, 28).atTime(17, 5)));
        assertTrue(horario2018.encerrouDepoisDoLimite("AC", 1392, LocalDate.of(2018, 10, 28).atTime(17, 11)));
    }

    @Test
    void recusaToleranciaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> new HorarioDaVotacao(DIA, Duration.ofMinutes(-1)));
    }
}
