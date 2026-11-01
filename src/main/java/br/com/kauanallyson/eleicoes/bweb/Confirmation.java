/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.io.Console;

/** Pergunta ao usuário antes de uma ação cara, como baixar centenas de MB. */
@FunctionalInterface
public interface Confirmation {

    boolean confirm(String question);

    /**
     * Com {@code autoConfirm} ({@code --sim} na linha de comando), confirma sem perguntar. Sem terminal
     * interativo (ex.: CI) e sem {@code --sim}, recusa: nunca baixa nada sem o usuário pedir.
     */
    static Confirmation fromTerminal(boolean autoConfirm) {
        if (autoConfirm) return question -> true;
        Console console = System.console();
        if (console == null) return question -> false;
        return question -> {
            var answer = console.readLine("%s [s/N] ", question);
            return answer != null && answer.trim().toLowerCase().matches("s|sim|y|yes");
        };
    }
}
