/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.Comparecimento;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoEstado;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Como cada {@link TipoDeRelatorio} monta a sua tabela a partir da apuração, sem I/O. */
public final class Respostas {

    private Respostas() { }

    static Tabela votosPorEstado(ResultadoApuracao boletins) {
        var linhas = boletins.estados().stream()
                .flatMap(estado -> estado.votos().emOrdem()
                        .map(e -> List.<Object>of(estado.uf(), e.getKey(), e.getValue(), new Percentual(estado.votos().percentual(e.getKey())))))
                .toList();
        return new Tabela("votos_por_estado.csv", List.of("UF", "CANDIDATO", "VOTOS", "PERCENTUAL"), linhas);
    }

    static Tabela votosTotais(ResultadoApuracao boletins) {
        var linhas = boletins.somarEstados(ResultadoEstado::votos).emOrdem()
                .map(e -> List.<Object>of(e.getKey(), e.getValue()))
                .toList();
        return new Tabela("votos_totais.csv", List.of("CANDIDATO", "VOTOS"), linhas);
    }

    /** Ordenado por percentual, com o total do país (BR) na última linha. */
    static Tabela votosDepoisDoFimDaVotacao(ResultadoApuracao boletins) {
        var porUf = boletins.estados().stream()
                .map(estado -> new DepoisDoFim(estado.uf(), estado.votosDepoisDoFimDaVotacao().total(), estado.votos().total()))
                .sorted(Comparator.comparing(DepoisDoFim::percentual).reversed())
                .map(DepoisDoFim::linha);
        long depois = boletins.estados().stream().mapToLong(e -> e.votosDepoisDoFimDaVotacao().total()).sum();
        long total = boletins.estados().stream().mapToLong(e -> e.votos().total()).sum();

        return new Tabela("votos_depois_do_fim_da_votacao_%s.csv".formatted(sufixoLimite(boletins)),
                List.of("UF", "VOTOS_DEPOIS_DO_FIM_DA_VOTACAO", "PERCENTUAL"),
                Stream.concat(porUf, Stream.of(new DepoisDoFim("BR", depois, total).linha())).toList());
    }

    private record DepoisDoFim(String uf, long depois, long total) {
        Percentual percentual() {
            return Percentual.de(depois, total);
        }

        List<Object> linha() {
            return List.of(uf, depois, percentual());
        }
    }

    /** Cada percentual é calculado dentro do seu grupo de seções (até / após o limite). */
    static Tabela candidatosAteEDepoisDoFimDaVotacao(ResultadoApuracao boletins) {
        var ate = boletins.somarEstados(ResultadoEstado::votosAteFimDaVotacao);
        var depois = boletins.somarEstados(ResultadoEstado::votosDepoisDoFimDaVotacao);
        var s = sufixoLimite(boletins);

        var linhas = Stream.concat(
                ate.emOrdem().map(e -> List.<Object>of(e.getKey(), e.getValue(), new Percentual(ate.percentual(e.getKey())),
                        depois.de(e.getKey()), new Percentual(depois.percentual(e.getKey())))),
                Stream.of(List.<Object>of("TOTAL", ate.total(), Percentual.CEM, depois.total(), Percentual.CEM)));

        return new Tabela("candidatos_ate_e_apos_fim_da_votacao_%s.csv".formatted(s),
                List.of("CANDIDATO", "VOTOS_ATE_" + s, "PERCENTUAL_ATE_" + s, "VOTOS_APOS_" + s, "PERCENTUAL_APOS_" + s),
                linhas.toList());
    }

    /** Ordenado pelo percentual de abstenção, com o total do país (BR) na última linha. */
    static Tabela comparecimentoPorEstado(ResultadoApuracao boletins) {
        var porUf = boletins.estados().stream()
                .sorted(Comparator.comparing((ResultadoEstado e) -> percentualDeAbstencao(e.comparecimento())).reversed())
                .map(estado -> linhaDeComparecimento(estado.uf(), estado.comparecimento()));
        var pais = boletins.estados().stream()
                .map(ResultadoEstado::comparecimento)
                .reduce(Comparecimento.NENHUM, Comparecimento::mais);

        return new Tabela("comparecimento_e_abstencao_por_estado.csv",
                List.of("UF", "APTOS", "COMPARECIMENTO", "PERCENTUAL_COMPARECIMENTO", "ABSTENCOES", "PERCENTUAL_ABSTENCOES"),
                Stream.concat(porUf, Stream.of(linhaDeComparecimento("BR", pais))).toList());
    }

    private static Percentual percentualDeAbstencao(Comparecimento c) {
        return Percentual.de(c.abstencoes(), c.aptos());
    }

    private static List<Object> linhaDeComparecimento(String uf, Comparecimento c) {
        return List.of(uf, c.aptos(), c.comparecimento(), Percentual.de(c.comparecimento(), c.aptos()),
                c.abstencoes(), percentualDeAbstencao(c));
    }

    /** Ex.: "1710" para 17:10, para o nome do arquivo deixar claro qual corte foi usado. */
    private static String sufixoLimite(ResultadoApuracao boletins) {
        return boletins.horario().limite().toString().replace(":", "");
    }
}
