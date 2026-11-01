/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

/** Votos de um candidato em uma UF e o percentual sobre o total da UF. */
record VotosNoEstado(String uf, String candidato, long votos, double percentual) { }
