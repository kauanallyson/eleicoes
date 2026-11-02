/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

/** Pergunta ao usuário antes de uma ação cara, como baixar centenas de MB. */
@FunctionalInterface
public interface Confirmation {

    boolean confirm(String question);
}
