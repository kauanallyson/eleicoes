/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.util.List;
import java.util.function.Function;

public record ResultadoApuracao(List<ResultadoEstado> estados, HorarioDaVotacao horario) {

    public <K> Contagem<K> somarEstados(Function<ResultadoEstado, Contagem<K>> campo) {
        return Contagem.somar(estados.stream().map(campo).toList());
    }
}
