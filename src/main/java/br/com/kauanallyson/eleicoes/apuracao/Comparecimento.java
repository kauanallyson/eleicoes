/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

/** Eleitores aptos e quantos compareceram; quem não compareceu se absteve. */
public record Comparecimento(long aptos, long comparecimento) {

    public static final Comparecimento NENHUM = new Comparecimento(0, 0);

    public Comparecimento {
        if (comparecimento > aptos) {
            throw new IllegalArgumentException("comparecimento (%d) maior que aptos (%d)".formatted(comparecimento, aptos));
        }
    }

    public long abstencoes() {
        return aptos - comparecimento;
    }

    public Comparecimento mais(Comparecimento outro) {
        return new Comparecimento(aptos + outro.aptos, comparecimento + outro.comparecimento);
    }
}
