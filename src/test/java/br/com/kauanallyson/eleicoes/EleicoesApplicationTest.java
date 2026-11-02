/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EleicoesApplicationTest {

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private final PrintStream erro = new PrintStream(saida, true, StandardCharsets.UTF_8);

    private String erro() {
        return saida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void argumentoInvalidoEhUmaMensagemCurtaComCodigo2() {
        assertEquals(EleicoesApplication.USO_INVALIDO, EleicoesApplication.executar(new String[]{"--relatorios=mapa"}, erro));

        assertTrue(erro().startsWith("erro: relatório desconhecido: mapa"), erro());
        assertTrue(erro().contains("por-estado"), erro());
        assertFalse(erro().contains("\tat "), "sem stack trace: " + erro());
    }

    @Test
    void anoSemEleicaoParaPresidenteTambemEhErroDeUso() {
        assertEquals(EleicoesApplication.USO_INVALIDO, EleicoesApplication.executar(new String[]{"--ano=2024"}, erro));

        assertEquals("erro: só eleições para Presidente (2018, 2022, 2026...), não 2024\n", erro());
    }

    @Test
    void mensagemPulaOEmbrulhoDoUncheckedIOException() {
        var e = new UncheckedIOException(new IOException("TSE respondeu 404 para https://x"));

        assertEquals("TSE respondeu 404 para https://x", EleicoesApplication.mensagem(e));
    }

    @Test
    void erroDeRedeSemMensagemAindaDizOQueFoi() {
        assertEquals("falha de rede ou de arquivo (ConnectException)", EleicoesApplication.mensagem(new ConnectException()));
    }
}
