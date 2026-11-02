/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

/** Código de saída do programa, para scripts saberem o que aconteceu. */
enum CodigoDeSaida {

    /** Argumento inválido: ano, turno, relatório ou opção desconhecida. */
    USO_INVALIDO(2),
    /** Rede, arquivo, hash do TSE ou download não autorizado. */
    FALHOU(1);

    private final int valor;

    CodigoDeSaida(int valor) {
        this.valor = valor;
    }

    void sair() {
        System.exit(valor);
    }
}
