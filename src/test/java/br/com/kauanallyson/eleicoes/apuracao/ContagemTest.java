/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContagemTest {

    @Test
    void agrupaSomandoAQuantidadeDeCadaElemento() {
        record Linha(String nome, int votos) { }
        var contagem = Stream.of(new Linha("A", 3), new Linha("B", 1), new Linha("A", 2))
                .collect(Contagem.por(Linha::nome, Linha::votos));

        assertEquals(Contagem.de(Map.of("A", 5L, "B", 1L)), contagem);
        assertEquals(6, contagem.total());
    }

    @Test
    void somaContagensDeVariosEstados() {
        var soma = Contagem.somar(List.of(Contagem.de(Map.of("A", 5L)), Contagem.de(Map.of("A", 1L, "B", 4L))));

        assertEquals(Contagem.de(Map.of("A", 6L, "B", 4L)), soma);
    }

    @Test
    void ordenaDoMaisParaOMenosVotado() {
        var ordem = Contagem.de(Map.of("A", 1L, "B", 9L, "C", 5L)).emOrdem().map(Map.Entry::getKey).toList();

        assertEquals(List.of("B", "C", "A"), ordem);
    }

    @Test
    void percentualSobreOTotal() {
        var contagem = Contagem.de(Map.of("A", 1L, "B", 3L));

        assertEquals(25.0, contagem.percentual("A"));
        assertEquals(0.0, contagem.percentual("ausente"));
    }

    @Test
    void contagemVaziaNaoDividePorZero() {
        Contagem<String> vazia = Contagem.vazia();

        assertTrue(vazia.isEmpty());
        assertEquals(0, vazia.total());
        assertEquals(0.0, vazia.percentual("A"));
    }
}
