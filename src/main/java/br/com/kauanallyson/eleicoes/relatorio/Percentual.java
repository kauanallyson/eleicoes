/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

/** Uma célula de percentual (0 a 100) numa {@link Tabela}; o adapter decide como formatar. */
public record Percentual(double valor) implements Comparable<Percentual> {

    public static final Percentual CEM = new Percentual(100);

    public static Percentual de(long parte, long total) {
        return new Percentual(total == 0 ? 0 : 100.0 * parte / total);
    }

    @Override
    public int compareTo(Percentual outro) {
        return Double.compare(valor, outro.valor);
    }
}
