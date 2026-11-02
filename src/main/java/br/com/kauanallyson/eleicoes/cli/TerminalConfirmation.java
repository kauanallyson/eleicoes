/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.cli;

import br.com.kauanallyson.eleicoes.bweb.Confirmation;

import java.io.Console;

/** {@link Confirmation} que pergunta no terminal. */
public final class TerminalConfirmation {

    private TerminalConfirmation() { }

    /**
     * Com {@code autoConfirm} ({@code --sim} na linha de comando), confirma sem perguntar. Sem terminal
     * interativo (ex.: CI) e sem {@code --sim}, recusa: nunca baixa nada sem o usuário pedir.
     */
    public static Confirmation of(boolean autoConfirm) {
        if (autoConfirm) return question -> true;
        Console console = System.console();
        if (console == null) return question -> false;
        return question -> {
            var answer = console.readLine("%s [s/N] ", question);
            return answer != null && answer.trim().toLowerCase().matches("s|sim|y|yes");
        };
    }
}
