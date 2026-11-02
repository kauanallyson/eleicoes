/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Grava os relatórios escolhidos, substituindo os CSVs de execuções anteriores. */
public class Relatorio {

    private final ResultadoCsvWriter csv;
    private final Set<TipoDeRelatorio> tipos;
    private final Consumer<String> log;

    private Relatorio(Builder builder) {
        this.csv = builder.csv;
        this.tipos = builder.tipos;
        this.log = builder.log;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void gravar(ResultadoApuracao resultado) throws IOException {
        csv.prepararDiretorio();
        int prontos = 0;
        for (var tipo : tipos) {
            var tabela = tipo.gerar(resultado);
            csv.gravar(tabela);
            prontos++;
            log.accept("[relatórios] %3d%% (%d/%d) %s".formatted(100 * prontos / tipos.size(), prontos, tipos.size(),
                    tabela.nome()));
        }
    }

    public static class Builder {

        public static final Path DIRETORIO_PADRAO = Path.of("resultados");

        private ResultadoCsvWriter csv = new ResultadoCsvWriter(DIRETORIO_PADRAO);
        private Set<TipoDeRelatorio> tipos = EnumSet.allOf(TipoDeRelatorio.class);
        private Consumer<String> log = mensagem -> { };

        private Builder() { }

        public Builder diretorio(Path diretorio) {
            this.csv = new ResultadoCsvWriter(Objects.requireNonNull(diretorio, "diretorio"));
            return this;
        }

        /** Padrão: todos. Os CSVs saem na ordem do catálogo, qualquer que seja a ordem pedida. */
        public Builder tipos(Collection<TipoDeRelatorio> tipos) {
            Objects.requireNonNull(tipos, "tipos");
            if (tipos.isEmpty()) throw new IllegalArgumentException("escolha ao menos um relatório");
            this.tipos = EnumSet.copyOf(tipos);
            return this;
        }

        /** Opcional: recebe o andamento, uma linha por CSV gravado, com o percentual concluído. */
        public Builder log(Consumer<String> log) {
            this.log = Objects.requireNonNull(log, "log");
            return this;
        }

        public Relatorio build() {
            return new Relatorio(this);
        }
    }
}
