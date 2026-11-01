/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

/** Votos de um candidato nas seções encerradas até e após o fim da votação (com tolerância). */
record CandidatoPorHorario(String candidato, long votosAte, double percentualAte, long votosApos, double percentualApos) { }
