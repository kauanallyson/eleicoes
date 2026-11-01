/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.time.LocalDateTime;

/**
 * Votos de um votável (candidato, branco ou nulo) num cargo, numa seção: uma linha do boletim de urna.
 *
 * @param encerramentoLocal horário de encerramento da seção no fuso local; {@code null} nas linhas sem seção
 *                          (ex.: juntas apuradoras do exterior)
 */
public record VotosNaSecao(String uf, int municipio, String cargo, String votavel, long quantidade,
                           LocalDateTime encerramentoLocal) { }
