/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EleicaoTest {

    @Test
    void primeiroTurnoNoPrimeiroDomingoESegundoNoUltimoDomingoDeOutubro() {
        assertEquals(LocalDate.of(2026, 10, 4), new Eleicao(2026, 1).data());
        assertEquals(LocalDate.of(2026, 10, 25), new Eleicao(2026, 2).data());
        assertEquals(LocalDate.of(2022, 10, 2), new Eleicao(2022, 1).data());
        assertEquals(LocalDate.of(2022, 10, 30), new Eleicao(2022, 2).data());
    }

    @Test
    void pastaPorAnoETurno() {
        assertEquals(Path.of("2026", "2t"), new Eleicao(2026, 2).pasta());
    }

    @Test
    void recusaAnoSemEleicaoGeralETurnoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Eleicao(2025, 1));
        assertThrows(IllegalArgumentException.class, () -> new Eleicao(2024, 1));
        assertThrows(IllegalArgumentException.class, () -> new Eleicao(2026, 3));
    }
}
