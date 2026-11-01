/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Grava todas as respostas da apuração, substituindo as de execuções anteriores. */
public class Relatorio {

    private final ResultadoCsvWriter csv;

    private Relatorio(Builder builder) {
        this.csv = builder.csv;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void gravar(ResultadoApuracao boletins) throws IOException {
        csv.gravar(Respostas.de(boletins));
    }

    public static class Builder {

        public static final Path DIRETORIO_PADRAO = Path.of("resultados");

        private ResultadoCsvWriter csv = new ResultadoCsvWriter(DIRETORIO_PADRAO);

        private Builder() { }

        public Builder diretorio(Path diretorio) {
            this.csv = new ResultadoCsvWriter(Objects.requireNonNull(diretorio, "diretorio"));
            return this;
        }

        public Relatorio build() {
            return new Relatorio(this);
        }
    }
}
