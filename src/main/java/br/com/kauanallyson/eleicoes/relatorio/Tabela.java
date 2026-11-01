/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import java.util.List;

/**
 * Uma resposta da apuração, pronta para ser gravada.
 *
 * @param linhas células tipadas: {@code Double} é percentual (0 a 100), {@code Boolean} é sim/não,
 *               o resto é gravado como texto
 */
public record Tabela(String nome, List<String> cabecalho, List<List<Object>> linhas) {

    public Tabela {
        cabecalho = List.copyOf(cabecalho);
        linhas = List.copyOf(linhas);
    }
}
