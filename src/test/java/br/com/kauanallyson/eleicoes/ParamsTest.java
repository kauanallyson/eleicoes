/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParametrosTest {

    @Test
    void semArgumentosEhO2oTurnoDe2026SemAutorizarDownload() {
        assertEquals(new Parametros(new Eleicao(2026, 2), false), Parametros.de(new String[0]));
    }

    @Test
    void anoTurnoESim() {
        assertEquals(new Parametros(new Eleicao(2022, 1), true), Parametros.de(new String[]{"--turno=1", "--sim", "--ano=2022"}));
    }

    @Test
    void argumentoDesconhecidoOuInvalidoEhErro() {
        assertThrows(IllegalArgumentException.class, () -> Parametros.de(new String[]{"--ano"}));
        assertThrows(IllegalArgumentException.class, () -> Parametros.de(new String[]{"--ano=dois mil"}));
        assertThrows(IllegalArgumentException.class, () -> Parametros.de(new String[]{"--turno=3"}));
    }
}
