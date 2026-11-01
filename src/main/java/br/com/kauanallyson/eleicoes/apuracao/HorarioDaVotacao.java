/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * O dia e o fim da votação (horário de Brasília) e a tolerância para o encerramento da urna.
 * Recebe os horários no fuso local da seção, como vêm no boletim de urna.
 */
public final class HorarioDaVotacao {

    public static final LocalTime FIM_DA_VOTACAO = LocalTime.of(17, 0);
    public static final LocalDate SEGUNDO_TURNO_2026 = LocalDate.of(2026, 10, 25);
    /** O TSE considera 17h10 o horário estimado para os procedimentos de encerramento da urna. */
    public static final Duration TOLERANCIA_PADRAO = Duration.ofMinutes(10);

    private final LocalDate data;
    private final Duration tolerancia;
    private final FusoHorario fusoHorario = new FusoHorario();

    /**
     * @param tolerancia tempo dado ao mesário para encerrar a urna depois do fim da votação; seções que
     *                   fecharam dentro dessa tolerância não contam como encerradas após o fim
     */
    public HorarioDaVotacao(LocalDate data, Duration tolerancia) {
        this.data = Objects.requireNonNull(data, "data");
        Objects.requireNonNull(tolerancia, "tolerancia");
        if (tolerancia.isNegative()) throw new IllegalArgumentException("tolerância não pode ser negativa: " + tolerancia);
        this.tolerancia = tolerancia;
    }

    public static HorarioDaVotacao padrao() {
        return new HorarioDaVotacao(SEGUNDO_TURNO_2026, TOLERANCIA_PADRAO);
    }

    public LocalDate data() {
        return data;
    }

    public LocalTime fim() {
        return FIM_DA_VOTACAO;
    }

    public Duration tolerancia() {
        return tolerancia;
    }

    public LocalTime limite() {
        return FIM_DA_VOTACAO.plus(tolerancia);
    }

    public boolean encerrouDepoisDoLimite(String uf, int municipio, LocalDateTime encerramentoLocal) {
        return emBrasilia(uf, municipio, encerramentoLocal).isAfter(limite());
    }

    private LocalTime emBrasilia(String uf, int municipio, LocalDateTime horarioLocal) {
        return fusoHorario.emBrasilia(uf, municipio, horarioLocal).toLocalTime();
    }
}
