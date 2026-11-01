/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;

/**
 * Argumentos da linha de comando: {@code --ano=2026 --turno=2 --sim}. Ano e turno têm como padrão o
 * 2º turno de 2026; {@code --sim} autoriza o download dos boletins sem perguntar.
 */
public record Params(Eleicao eleicao, boolean autoConfirm) {

    public static final String USO = "uso: [--ano=2026] [--turno=1|2] [--sim]";

    public static Params parse(String[] args) {
        int ano = 2026, turno = 2;
        boolean autoConfirm = false;
        for (var arg : args) {
            if (arg.equals("--sim")) autoConfirm = true;
            else if (arg.startsWith("--ano=")) ano = inteiro(arg, "--ano=");
            else if (arg.startsWith("--turno=")) turno = inteiro(arg, "--turno=");
            else throw new IllegalArgumentException("argumento desconhecido: " + arg + "\n" + USO);
        }
        return new Params(new Eleicao(ano, turno), autoConfirm);
    }

    private static int inteiro(String arg, String prefixo) {
        try {
            return Integer.parseInt(arg.substring(prefixo.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido em " + arg + "\n" + USO);
        }
    }
}
