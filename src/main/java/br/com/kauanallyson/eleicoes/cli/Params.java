/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.cli;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;
import br.com.kauanallyson.eleicoes.relatorio.TipoDeRelatorio;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Argumentos da linha de comando: {@code --ano=2026 --turno=2 --relatorios=totais,por-estado --sim}. Ano e turno
 * têm como padrão o 2º turno de 2026 e, sem {@code --relatorios}, saem todos os relatórios; {@code --sim} autoriza
 * o download dos boletins sem perguntar.
 */
public record Params(Eleicao eleicao, boolean autoConfirm, Set<TipoDeRelatorio> relatorios) {

    public static final String USO = "uso: [--ano=2026] [--turno=1|2] [--relatorios=nome,nome...] [--sim]\n"
            + TipoDeRelatorio.catalogo();

    public Params {
        relatorios = Set.copyOf(relatorios);
    }

    public static Params parse(String[] args) {
        int ano = 2026, turno = 2;
        boolean autoConfirm = false;
        Set<TipoDeRelatorio> relatorios = EnumSet.allOf(TipoDeRelatorio.class);
        for (var arg : args) {
            if (arg.equals("--sim")) autoConfirm = true;
            else if (arg.startsWith("--ano=")) ano = inteiro(arg, "--ano=");
            else if (arg.startsWith("--turno=")) turno = inteiro(arg, "--turno=");
            else if (arg.startsWith("--relatorios=")) relatorios = relatorios(arg.substring("--relatorios=".length()));
            else throw new IllegalArgumentException("argumento desconhecido: " + arg + "\n" + USO);
        }
        return new Params(new Eleicao(ano, turno), autoConfirm, relatorios);
    }

    /** Nomes separados por vírgula; espaços e repetições são ignorados. */
    private static Set<TipoDeRelatorio> relatorios(String nomes) {
        var tipos = Arrays.stream(nomes.split(","))
                .map(String::trim)
                .filter(nome -> !nome.isEmpty())
                .map(TipoDeRelatorio::porNome)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoDeRelatorio.class)));
        if (tipos.isEmpty()) throw new IllegalArgumentException("--relatorios vazio\n" + TipoDeRelatorio.catalogo());
        return tipos;
    }

    private static int inteiro(String arg, String prefixo) {
        try {
            return Integer.parseInt(arg.substring(prefixo.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido em " + arg + "\n" + USO);
        }
    }
}
