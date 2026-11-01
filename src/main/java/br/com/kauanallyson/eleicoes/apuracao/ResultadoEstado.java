/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.util.Map;

/** Resultado da apuração de um arquivo (uma UF). */
public record ResultadoEstado(String uf,
                              Map<Candidato, Long> votos,
                              Map<String, Long> votosAteFimDaVotacao,
                              Map<String, Long> votosDepoisDoFimDaVotacao) {

    public long totalDeVotos() {
        return Votos.soma(votos);
    }

    public long totalDepoisDoFimDaVotacao() {
        return Votos.soma(votosDepoisDoFimDaVotacao);
    }
}
