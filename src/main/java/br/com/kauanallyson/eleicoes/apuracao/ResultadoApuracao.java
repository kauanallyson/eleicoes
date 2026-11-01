/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ResultadoApuracao(List<ResultadoEstado> estados, LocalTime fimDaVotacao, Duration tolerancia) {

    public LocalTime limiteComTolerancia() {
        return fimDaVotacao.plus(tolerancia);
    }

    public <K> Map<K, Long> somarEstados(Function<ResultadoEstado, Map<K, Long>> campo) {
        return estados.stream()
                .flatMap(estado -> campo.apply(estado).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));
    }
}
