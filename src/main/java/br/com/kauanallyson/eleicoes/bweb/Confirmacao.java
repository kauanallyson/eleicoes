/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.io.Console;

/** Pergunta ao usuário antes de uma ação cara, como baixar centenas de MB. */
@FunctionalInterface
public interface Confirmacao {

    boolean confirmar(String pergunta);

    /**
     * Com {@code sim} (o {@code --sim} da linha de comando), confirma sem perguntar. Sem terminal interativo (ex.: CI) e sem
     * {@code --sim}, recusa: nunca baixa nada sem o usuário pedir.
     */
    static Confirmacao doTerminal(boolean sim) {
        if (sim) return pergunta -> true;
        Console console = System.console();
        if (console == null) return pergunta -> false;
        return pergunta -> {
            var resposta = console.readLine("%s [s/N] ", pergunta);
            return resposta != null && resposta.trim().toLowerCase().matches("s|sim|y|yes");
        };
    }
}
