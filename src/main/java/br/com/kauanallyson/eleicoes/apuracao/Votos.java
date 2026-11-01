/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.util.Map;

/** Operações sobre mapas de votos (chave → quantidade). */
public final class Votos {

    private Votos() { }

    public static long soma(Map<?, Long> votos) {
        return votos.values().stream().mapToLong(Long::longValue).sum();
    }
}
