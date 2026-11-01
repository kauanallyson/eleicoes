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
 * O dia e o fim da votação (17h) e a tolerância para o encerramento da urna. Recebe os horários no fuso local
 * da seção, como vêm no boletim de urna. Desde 2022 a votação segue o horário de Brasília em todo o
 * país; antes, cada seção encerrava às 17h do seu fuso local.
 */
public final class HorarioDaVotacao {

    public static final LocalTime FIM_DA_VOTACAO = LocalTime.of(17, 0);
    /** O TSE considera 17h10 o horário estimado para os procedimentos de encerramento da urna. */
    public static final Duration TOLERANCIA_PADRAO = Duration.ofMinutes(10);
    private static final int PRIMEIRO_ANO_COM_HORARIO_DE_BRASILIA = 2022;

    private final LocalDate data;
    private final Duration tolerancia;
    private final boolean horarioDeBrasilia;
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
        this.horarioDeBrasilia = data.getYear() >= PRIMEIRO_ANO_COM_HORARIO_DE_BRASILIA;
    }

    public static HorarioDaVotacao da(Eleicao eleicao, Duration tolerancia) {
        return new HorarioDaVotacao(eleicao.data(), tolerancia);
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
        return horarioDoFim(uf, municipio, encerramentoLocal).isAfter(limite());
    }

    /** Horário no mesmo relógio do fim da votação: Brasília desde 2022, o fuso local da seção antes. */
    private LocalTime horarioDoFim(String uf, int municipio, LocalDateTime horarioLocal) {
        if (!horarioDeBrasilia) return horarioLocal.toLocalTime();
        return fusoHorario.emBrasilia(uf, municipio, horarioLocal).toLocalTime();
    }
}
