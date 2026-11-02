/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

/** Apuração de uma UF. */
public record ResultadoEstado(String uf,
                              Contagem<String> votos,
                              Contagem<String> votosAteFimDaVotacao,
                              Contagem<String> votosDepoisDoFimDaVotacao,
                              Comparecimento comparecimento) {
}
