/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Uma eleição geral (2018, 2022, 2026...), a que elege o Presidente: ano e turno. Pela Constituição (art. 77),
 * o 1º turno é no primeiro domingo de outubro e o 2º turno no último domingo de outubro.
 */
public record Eleicao(int ano, int turno) {

    public Eleicao {
        if (ano % 4 != 2) {
            throw new IllegalArgumentException("só eleições para Presidente (2018, 2022, 2026...), não " + ano);
        }
        if (turno != 1 && turno != 2) throw new IllegalArgumentException("turno deve ser 1 ou 2: " + turno);
    }

    public LocalDate data() {
        var outubro = LocalDate.of(ano, Month.OCTOBER, 1);
        return turno == 1
                ? outubro.with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
                : outubro.with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));
    }

    /** Subpasta dos dados desta eleição, ex.: {@code 2026/2t}. */
    public Path pasta() {
        return Path.of(String.valueOf(ano), turno + "t");
    }

    @Override
    public String toString() {
        return "%d, %dº turno (%s)".formatted(ano, turno, data().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
}
