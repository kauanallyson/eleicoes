/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

/** Votos de uma UF em seções encerradas depois do fim da votação (com tolerância). */
record VotosDepoisDoFim(String uf, long votos, double percentual) { }
