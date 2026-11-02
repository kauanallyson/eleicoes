/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EleicoesApplicationTest {

    @Test
    void duracaoEmSegundosOuMinutos() {
        assertEquals("4,8 s", EleicoesApplication.duracao(Duration.ofMillis(4_812)));
        assertEquals("2 min 29 s", EleicoesApplication.duracao(Duration.ofSeconds(149)));
    }
}
