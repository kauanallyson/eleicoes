/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoEstado;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

public class Relatorio {

    private final ResultadoCsvWriter csv;

    private Relatorio(Builder builder) {
        this.csv = builder.csv;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public static final Path DIRETORIO_PADRAO = Path.of("resultados");

        private ResultadoCsvWriter csv = new ResultadoCsvWriter(DIRETORIO_PADRAO);

        private Builder() { }

        public Builder diretorio(Path diretorio) {
            this.csv = new ResultadoCsvWriter(Objects.requireNonNull(diretorio, "diretorio"));
            return this;
        }

        public Builder csv(ResultadoCsvWriter csv) {
            this.csv = Objects.requireNonNull(csv, "csv");
            return this;
        }

        public Relatorio build() {
            return new Relatorio(this);
        }
    }

    public void limparResultadosAnteriores() throws IOException {
        csv.limparResultadosAnteriores();
    }

    public void votosPorEstado(ResultadoApuracao resultado) {
        var linhas = resultado.estados().stream()
                .flatMap(estado -> {
                    var votos = estado.votos();
                    return votos.emOrdem()
                            .map(e -> new VotosNoEstado(estado.uf(), e.getKey().nome(), e.getValue(), votos.percentual(e.getKey())));
                });

        csv.write("votos_por_estado.csv", Stream.concat(
                Stream.<String[]>of(new String[]{"UF", "CANDIDATO", "VOTOS", "PERCENTUAL"}),
                linhas.map(l -> new String[]{l.uf(), l.candidato(), String.valueOf(l.votos()),
                        csv.percentual(l.percentual())})).toList());
    }

    public void votosTotais(ResultadoApuracao resultado) {
        csv.write("votos_totais.csv", Stream.concat(
                Stream.<String[]>of(new String[]{"CANDIDATO", "VOTOS"}),
                resultado.somarEstados(ResultadoEstado::votos).emOrdem()
                        .map(e -> new String[]{e.getKey().nome(), e.getValue().toString()})).toList());
    }

    public void votosDepoisDoFimDaVotacao(ResultadoApuracao resultado) {
        var linhas = resultado.estados().stream()
                .map(estado -> new VotosDepoisDoFim(estado.uf(), estado.votosDepoisDoFimDaVotacao().total(),
                        100.0 * estado.votosDepoisDoFimDaVotacao().total() / estado.votos().total()))
                .sorted(Comparator.comparingDouble(VotosDepoisDoFim::percentual).reversed())
                .toList();
        long totalDepois = linhas.stream().mapToLong(VotosDepoisDoFim::votos).sum();
        long totalGeral = resultado.estados().stream().mapToLong(estado -> estado.votos().total()).sum();
        var brasil = new VotosDepoisDoFim("BR", totalDepois, 100.0 * totalDepois / totalGeral);

        csv.write("votos_depois_do_fim_da_votacao_%s.csv".formatted(sufixoLimite(resultado)), Stream.concat(
                Stream.<String[]>of(new String[]{"UF", "VOTOS_DEPOIS_DO_FIM_DA_VOTACAO", "PERCENTUAL"}),
                Stream.concat(linhas.stream(), Stream.of(brasil))
                        .map(l -> new String[]{l.uf(), String.valueOf(l.votos()), csv.percentual(l.percentual())}))
                .toList());
    }

    /** Cada percentual é calculado dentro do seu grupo de seções (até / após o limite). */
    public void candidatosAteEDepoisDoFimDaVotacao(ResultadoApuracao resultado) {
        var ate = resultado.somarEstados(ResultadoEstado::votosAteFimDaVotacao);
        var depois = resultado.somarEstados(ResultadoEstado::votosDepoisDoFimDaVotacao);
        var linhas = ate.emOrdem()
                .map(e -> new CandidatoPorHorario(e.getKey(), e.getValue(), ate.percentual(e.getKey()),
                        depois.de(e.getKey()), depois.percentual(e.getKey())));
        var total = new CandidatoPorHorario("TOTAL", ate.total(), 100.0, depois.total(), 100.0);
        var s = sufixoLimite(resultado);

        csv.write("candidatos_ate_e_apos_fim_da_votacao_%s.csv".formatted(s), Stream.concat(
                Stream.<String[]>of(new String[]{"CANDIDATO", "VOTOS_ATE_" + s, "PERCENTUAL_ATE_" + s, "VOTOS_APOS_" + s, "PERCENTUAL_APOS_" + s}),
                Stream.concat(linhas, Stream.of(total))
                        .map(l -> new String[]{l.candidato(), String.valueOf(l.votosAte()), csv.percentual(l.percentualAte()),
                                String.valueOf(l.votosApos()), csv.percentual(l.percentualApos())}))
                .toList());
    }

    /** Ex.: "1710" para 17:10, para o nome do arquivo deixar claro qual corte foi usado. */
    private String sufixoLimite(ResultadoApuracao resultado) {
        return resultado.horario().limite().toString().replace(":", "");
    }
}
