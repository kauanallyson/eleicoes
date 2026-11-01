/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

public record ResultadoEstado(String uf,
                              Contagem<Candidato> votos,
                              Contagem<String> votosAteFimDaVotacao,
                              Contagem<String> votosDepoisDoFimDaVotacao) { }
