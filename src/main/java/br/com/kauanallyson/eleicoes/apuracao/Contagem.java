/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Quantidade de votos por chave (candidato, votável...). Imutável. */
public final class Contagem<K> {

    private static final Contagem<?> VAZIA = new Contagem<>(Map.of());

    private final Map<K, Long> votos;
    private final long total;

    private Contagem(Map<K, Long> votos) {
        this.votos = Map.copyOf(votos);
        this.total = this.votos.values().stream().mapToLong(Long::longValue).sum();
    }

    @SuppressWarnings("unchecked")
    public static <K> Contagem<K> vazia() {
        return (Contagem<K>) VAZIA;
    }

    public static <K> Contagem<K> de(Map<K, Long> votos) {
        return new Contagem<>(votos);
    }

    /** Agrupa os elementos pela chave somando a quantidade de cada um. */
    public static <T, K> Collector<T, ?, Contagem<K>> por(Function<T, K> chave, ToLongFunction<T> quantidade) {
        return Collectors.collectingAndThen(
                Collectors.groupingBy(chave, Collectors.summingLong(quantidade)),
                Contagem::new);
    }

    public static <K> Contagem<K> somar(Collection<Contagem<K>> contagens) {
        var soma = new HashMap<K, Long>();
        contagens.forEach(c -> c.votos.forEach((k, v) -> soma.merge(k, v, Long::sum)));
        return new Contagem<>(soma);
    }

    public long total() {
        return total;
    }

    public long de(K chave) {
        return votos.getOrDefault(chave, 0L);
    }

    /** Percentual (0 a 100) da chave sobre o total; 0 se a contagem estiver vazia. */
    public double percentual(K chave) {
        return total == 0 ? 0 : 100.0 * de(chave) / total;
    }

    /** Do mais votado para o menos votado. */
    public Stream<Map.Entry<K, Long>> emOrdem() {
        return votos.entrySet().stream().sorted(Map.Entry.<K, Long>comparingByValue().reversed());
    }

    public boolean isEmpty() {
        return votos.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Contagem<?> c && votos.equals(c.votos);
    }

    @Override
    public int hashCode() {
        return votos.hashCode();
    }

    @Override
    public String toString() {
        return votos.toString();
    }
}
