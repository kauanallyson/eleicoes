/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParamsTest {

    @Test
    void semArgumentosEhO2oTurnoDe2026SemAutorizarDownload() {
        assertEquals(new Params(new Eleicao(2026, 2), false), Params.parse(new String[0]));
    }

    @Test
    void anoTurnoESim() {
        assertEquals(new Params(new Eleicao(2022, 1), true), Params.parse(new String[]{"--turno=1", "--sim", "--ano=2022"}));
    }

    @Test
    void argumentoDesconhecidoOuInvalidoEhErro() {
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--ano"}));
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--ano=dois mil"}));
        assertThrows(IllegalArgumentException.class, () -> Params.parse(new String[]{"--turno=3"}));
    }
}
