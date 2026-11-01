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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.IO.println;

/** Exibe o top de cada resposta no terminal e grava o resultado completo em CSV. */
public class Relatorio {

    private final ResultadoCsvWriter csv;
    private final int top;

    private Relatorio(Builder builder) {
        this.csv = builder.csv;
        this.top = builder.top;
    }

    /** Cria um builder com os valores padrão: CSVs em {@code resultados/}, top 5 no terminal. */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /** Diretório padrão dos CSVs de resultado. */
        public static final Path DIRETORIO_PADRAO = Path.of("resultados");
        /** Quantidade padrão de linhas de cada resposta exibidas no terminal. */
        public static final int TOP_PADRAO = 5;

        private ResultadoCsvWriter csv = new ResultadoCsvWriter(DIRETORIO_PADRAO);
        private int top = TOP_PADRAO;

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

        /** Quantas linhas de cada resposta aparecem no terminal; o resultado completo vai para o CSV. */
        public Builder top(int top) {
            if (top <= 0) throw new IllegalArgumentException("top deve ser positivo: " + top);
            this.top = top;
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

    public void votosPorEstado(ResultadoApuracao resultado) {
        var linhas = resultado.estados().stream()
                .flatMap(estado -> {
                    long total = estado.totalDeVotos();
                    return maisVotadosPrimeiro(estado.votos())
                            .map(e -> new VotosNoEstado(estado.uf(), e.getKey().nome(), e.getValue(), 100.0 * e.getValue() / total));
                })
                .toList();
        var arquivo = "votos_por_estado.csv";

        println("===============================");
        println("VOTOS POR ESTADO (top %d estados com mais votos):".formatted(top));
        linhas.stream()
                .collect(Collectors.groupingBy(VotosNoEstado::uf, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, List<VotosNoEstado>> e) ->
                        e.getValue().stream().mapToLong(VotosNoEstado::votos).sum()).reversed())
                .limit(top)
                .forEach(e -> {
                    println(e.getKey() + ":");
                    e.getValue().stream().limit(top)
                            .forEach(l -> println("  %-20s %,12d %7.2f%%".formatted(l.candidato(), l.votos(), l.percentual())));
                });
        println(completoEm(arquivo));
        println("===============================");
        println();

        csv.write(arquivo, Stream.concat(
                Stream.<String[]>of(new String[]{"UF", "CANDIDATO", "VOTOS", "PERCENTUAL"}),
                linhas.stream().map(l -> new String[]{l.uf(), l.candidato(), String.valueOf(l.votos()),
                        csv.percentual(l.percentual())})).toList());
    }

    public void votosTotais(ResultadoApuracao resultado) {
        var total = maisVotadosPrimeiro(resultado.somarEstados(ResultadoEstado::votos)).toList();
        var arquivo = "votos_totais.csv";

        println("===============================");
        println("VOTOS TOTAIS (top %d):".formatted(top));
        total.stream().limit(top).forEach(e -> println("%-20s %,d".formatted(e.getKey().nome(), e.getValue())));
        println(completoEm(arquivo));
        println("===============================");
        println();

        csv.write(arquivo, Stream.concat(
                Stream.<String[]>of(new String[]{"CANDIDATO", "VOTOS"}),
                total.stream().map(e -> new String[]{e.getKey().nome(), e.getValue().toString()})).toList());
    }

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
        var arquivo = "votos_depois_do_fim_da_votacao_%s.csv".formatted(sufixoLimite(resultado));

        println("===========================================");
        println("VOTOS EM SEÇÕES ENCERRADAS DEPOIS DO FIM DA VOTAÇÃO");
        println("(fim às %s + tolerância de %d min = %s, horário de Brasília)".formatted(
                resultado.fimDaVotacao(), resultado.tolerancia().toMinutes(), resultado.limiteComTolerancia()));
        println("Top %d UFs por percentual:".formatted(top));
        println("%-4s %12s %8s".formatted("UF", "QTDE VOTOS", "%"));
        Stream.concat(linhas.stream().limit(top), Stream.of(brasil))
                .forEach(l -> println("%-4s %,12d %7.2f%%".formatted(l.uf(), l.votos(), l.percentual())));
        println(completoEm(arquivo));
        println("===========================================");
        println();

        csv.write(arquivo, Stream.concat(
                Stream.<String[]>of(new String[]{"UF", "VOTOS_DEPOIS_DO_FIM_DA_VOTACAO", "PERCENTUAL"}),
                Stream.concat(linhas.stream(), Stream.of(brasil))
                        .map(l -> new String[]{l.uf(), String.valueOf(l.votos()), csv.percentual(l.percentual())}))
                .toList());
    }

    public void candidatosAteEDepoisDoFimDaVotacao(ResultadoApuracao resultado) {
        var ate = resultado.somarEstados(ResultadoEstado::votosAteFimDaVotacao);
        var depois = resultado.somarEstados(ResultadoEstado::votosDepoisDoFimDaVotacao);
        long totalAte = Votos.soma(ate), totalDepois = Votos.soma(depois);

        var linhas = maisVotadosPrimeiro(ate)
                .map(e -> {
                    long qtDepois = depois.getOrDefault(e.getKey(), 0L);
                    return new CandidatoPorHorario(e.getKey(), e.getValue(), 100.0 * e.getValue() / totalAte,
                            qtDepois, 100.0 * qtDepois / totalDepois);
                })
                .toList();
        var total = new CandidatoPorHorario("TOTAL", totalAte, 100.0, totalDepois, 100.0);

        var limite = resultado.limiteComTolerancia().toString();
        var s = sufixoLimite(resultado);
        var arquivo = "candidatos_ate_e_apos_fim_da_votacao_%s.csv".formatted(s);
        var linha = "-".repeat(62);
        var formato = "%-18s %,12d %7.2f%%   %,12d %7.2f%%";

        println("=".repeat(62));
        println("PRESIDENTE - VOTOS PELO HORÁRIO DE ENCERRAMENTO DA SEÇÃO");
        println("Fim da votação: %s + %d min de tolerância = %s (Brasília)".formatted(
                resultado.fimDaVotacao(), resultado.tolerancia().toMinutes(), limite));
        println("% = participação do candidato dentro de cada grupo de seções");
        println();
        println("%-18s %21s   %21s".formatted("", "SEÇÕES ATÉ " + limite, "SEÇÕES APÓS " + limite));
        println("%-18s %12s %8s   %12s %8s".formatted("CANDIDATO", "VOTOS", "%", "VOTOS", "%"));
        println(linha);
        linhas.stream().limit(top)
                .forEach(l -> println(formato.formatted(l.candidato(), l.votosAte(), l.percentualAte(), l.votosApos(), l.percentualApos())));
        println(linha);
        println(formato.formatted(total.candidato(), total.votosAte(), total.percentualAte(), total.votosApos(), total.percentualApos()));
        println(completoEm(arquivo));
        println("=".repeat(62));
        println();

        csv.write(arquivo, Stream.concat(
                Stream.<String[]>of(new String[]{"CANDIDATO", "VOTOS_ATE_" + s, "PERCENTUAL_ATE_" + s, "VOTOS_APOS_" + s, "PERCENTUAL_APOS_" + s}),
                Stream.concat(linhas.stream(), Stream.of(total))
                        .map(l -> new String[]{l.candidato(), String.valueOf(l.votosAte()), csv.percentual(l.percentualAte()),
                                String.valueOf(l.votosApos()), csv.percentual(l.percentualApos())}))
                .toList());
    }

    private <K> Stream<Map.Entry<K, Long>> maisVotadosPrimeiro(Map<K, Long> votos) {
        return votos.entrySet().stream().sorted(Map.Entry.<K, Long>comparingByValue().reversed());
    }

    /** Ex.: "1715" para 17:15, para o nome do arquivo deixar claro qual corte foi usado. */
    private String sufixoLimite(ResultadoApuracao resultado) {
        return resultado.limiteComTolerancia().toString().replace(":", "");
    }

    private String completoEm(String arquivo) {
        return "(resultado completo em %s)".formatted(csv.caminho(arquivo));
    }
}
