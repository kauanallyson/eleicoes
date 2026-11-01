/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

public record ResultadoApuracao(List<ResultadoEstado> estados, LocalTime fimDaVotacao, Duration tolerancia) {

    public LocalTime limiteComTolerancia() {
        return fimDaVotacao.plus(tolerancia);
    }

    public <K> Contagem<K> somarEstados(Function<ResultadoEstado, Contagem<K>> campo) {
        return Contagem.somar(estados.stream().map(campo).toList());
    }
}
