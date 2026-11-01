/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Adapter de teste para {@link FonteDeVotos}: os registros de cada UF ficam em memória. */
record FonteEmMemoria<T>(Map<String, List<T>> porUf) implements FonteDeVotos<T> {

    @Override
    public List<String> ufs() {
        return List.copyOf(new TreeMap<>(porUf).keySet());
    }

    @Override
    public Stream<T> registros(String uf) {
        return porUf.getOrDefault(uf, List.of()).stream();
    }
}
