/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoEstado;
import br.com.kauanallyson.eleicoes.apuracao.Votos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Grava cada resposta da apuração em um CSV. */
public class Relatorio {

    private final ResultadoCsvWriter csv;

    private Relatorio(Builder builder) {
        this.csv = builder.csv;
    }

    /** Cria um builder com os valores padrão: CSVs em {@code resultados/}. */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /** Diretório padrão dos CSVs de resultado. */
        public static final Path DIRETORIO_PADRAO = Path.of("resultados");

        private ResultadoCsvWriter csv = new ResultadoCsvWriter(DIRETORIO_PADRAO);

        private Builder() { }

        /** Diretório onde os CSVs de resultado são gravados. */
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

    /** Apaga os CSVs de execuções anteriores, para o diretório ter só o resultado atual. */
    public void limparResultadosAnteriores() throws IOException {
        csv.limparResultadosAnteriores();
    }

    /** {@code votos_por_estado.csv}: votos de cada candidato por UF, com percentual sobre o total da UF. */
    public void votosPorEstado(ResultadoApuracao resultado) {
        var linhas = resultado.estados().stream()
                .flatMap(estado -> {
                    long total = estado.totalDeVotos();
                    return maisVotadosPrimeiro(estado.votos())
                            .map(e -> new VotosNoEstado(estado.uf(), e.getKey().nome(), e.getValue(), 100.0 * e.getValue() / total));
                });

        csv.write("votos_por_estado.csv", Stream.concat(
                Stream.<String[]>of(new String[]{"UF", "CANDIDATO", "VOTOS", "PERCENTUAL"}),
                linhas.map(l -> new String[]{l.uf(), l.candidato(), String.valueOf(l.votos()),
                        csv.percentual(l.percentual())})).toList());
    }

    /** {@code votos_totais.csv}: votos de cada candidato no país. */
    public void votosTotais(ResultadoApuracao resultado) {
        csv.write("votos_totais.csv", Stream.concat(
                Stream.<String[]>of(new String[]{"CANDIDATO", "VOTOS"}),
                maisVotadosPrimeiro(resultado.somarEstados(ResultadoEstado::votos))
                        .map(e -> new String[]{e.getKey().nome(), e.getValue().toString()})).toList());
    }

    /**
     * {@code votos_depois_do_fim_da_votacao_HHMM.csv}: votos de cada UF em seções encerradas depois do
     * fim da votação com tolerância, ordenado por percentual, com o total do país na última linha.
     */
    public void votosDepoisDoFimDaVotacao(ResultadoApuracao resultado) {
        // calcula cada percentual uma vez só, antes de ordenar
        var linhas = resultado.estados().stream()
                .map(estado -> new VotosDepoisDoFim(estado.uf(), estado.totalDepoisDoFimDaVotacao(),
                        100.0 * estado.totalDepoisDoFimDaVotacao() / estado.totalDeVotos()))
                .sorted(Comparator.comparingDouble(VotosDepoisDoFim::percentual).reversed())
                .toList();
        long totalDepois = linhas.stream().mapToLong(VotosDepoisDoFim::votos).sum();
        long totalGeral = resultado.estados().stream().mapToLong(ResultadoEstado::totalDeVotos).sum();
        var brasil = new VotosDepoisDoFim("BR", totalDepois, 100.0 * totalDepois / totalGeral);

        csv.write("votos_depois_do_fim_da_votacao_%s.csv".formatted(sufixoLimite(resultado)), Stream.concat(
                Stream.<String[]>of(new String[]{"UF", "VOTOS_DEPOIS_DO_FIM_DA_VOTACAO", "PERCENTUAL"}),
                Stream.concat(linhas.stream(), Stream.of(brasil))
                        .map(l -> new String[]{l.uf(), String.valueOf(l.votos()), csv.percentual(l.percentual())}))
                .toList());
    }

    /**
     * {@code candidatos_ate_e_apos_fim_da_votacao_HHMM.csv}: votos de cada candidato nas seções encerradas
     * até e após o fim da votação com tolerância; cada percentual é dentro do seu grupo de seções.
     */
    public void candidatosAteEDepoisDoFimDaVotacao(ResultadoApuracao resultado) {
        var ate = resultado.somarEstados(ResultadoEstado::votosAteFimDaVotacao);
        var depois = resultado.somarEstados(ResultadoEstado::votosDepoisDoFimDaVotacao);
        long totalAte = Votos.soma(ate), totalDepois = Votos.soma(depois);

        var linhas = maisVotadosPrimeiro(ate)
                .map(e -> {
                    long qtDepois = depois.getOrDefault(e.getKey(), 0L);
                    return new CandidatoPorHorario(e.getKey(), e.getValue(), 100.0 * e.getValue() / totalAte,
                            qtDepois, 100.0 * qtDepois / totalDepois);
                });
        var total = new CandidatoPorHorario("TOTAL", totalAte, 100.0, totalDepois, 100.0);
        var s = sufixoLimite(resultado);

        csv.write("candidatos_ate_e_apos_fim_da_votacao_%s.csv".formatted(s), Stream.concat(
                Stream.<String[]>of(new String[]{"CANDIDATO", "VOTOS_ATE_" + s, "PERCENTUAL_ATE_" + s, "VOTOS_APOS_" + s, "PERCENTUAL_APOS_" + s}),
                Stream.concat(linhas, Stream.of(total))
                        .map(l -> new String[]{l.candidato(), String.valueOf(l.votosAte()), csv.percentual(l.percentualAte()),
                                String.valueOf(l.votosApos()), csv.percentual(l.percentualApos())}))
                .toList());
    }

    private <K> Stream<Map.Entry<K, Long>> maisVotadosPrimeiro(Map<K, Long> votos) {
        return votos.entrySet().stream().sorted(Map.Entry.<K, Long>comparingByValue().reversed());
    }

    /** Ex.: "1710" para 17:10, para o nome do arquivo deixar claro qual corte foi usado. */
    private String sufixoLimite(ResultadoApuracao resultado) {
        return resultado.limiteComTolerancia().toString().replace(":", "");
    }
}
