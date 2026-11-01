/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;
import br.com.kauanallyson.eleicoes.apuracao.ResultadoEstado;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** As respostas da apuração como tabelas, sem I/O. */
public final class Respostas {

    private Respostas() { }

    public static List<Tabela> de(ResultadoApuracao boletins) {
        return List.of(
                votosPorEstado(boletins),
                votosTotais(boletins),
                votosDepoisDoFimDaVotacao(boletins),
                candidatosAteEDepoisDoFimDaVotacao(boletins));
    }

    static Tabela votosPorEstado(ResultadoApuracao boletins) {
        var linhas = boletins.estados().stream()
                .flatMap(estado -> estado.votos().emOrdem()
                        .map(e -> List.<Object>of(estado.uf(), e.getKey().nome(), e.getValue(), estado.votos().percentual(e.getKey()))))
                .toList();
        return new Tabela("votos_por_estado.csv", List.of("UF", "CANDIDATO", "VOTOS", "PERCENTUAL"), linhas);
    }

    static Tabela votosTotais(ResultadoApuracao boletins) {
        var linhas = boletins.somarEstados(ResultadoEstado::votos).emOrdem()
                .map(e -> List.<Object>of(e.getKey().nome(), e.getValue()))
                .toList();
        return new Tabela("votos_totais.csv", List.of("CANDIDATO", "VOTOS"), linhas);
    }

    /** Ordenado por percentual, com o total do país (BR) na última linha. */
    static Tabela votosDepoisDoFimDaVotacao(ResultadoApuracao boletins) {
        var porUf = boletins.estados().stream()
                .map(estado -> linhaDepoisDoFim(estado.uf(), estado.votosDepoisDoFimDaVotacao().total(), estado.votos().total()))
                .sorted(Comparator.comparingDouble((List<Object> l) -> (Double) l.get(2)).reversed());
        long depois = boletins.estados().stream().mapToLong(e -> e.votosDepoisDoFimDaVotacao().total()).sum();
        long total = boletins.estados().stream().mapToLong(e -> e.votos().total()).sum();

        return new Tabela("votos_depois_do_fim_da_votacao_%s.csv".formatted(sufixoLimite(boletins)),
                List.of("UF", "VOTOS_DEPOIS_DO_FIM_DA_VOTACAO", "PERCENTUAL"),
                Stream.concat(porUf, Stream.of(linhaDepoisDoFim("BR", depois, total))).toList());
    }

    private static List<Object> linhaDepoisDoFim(String uf, long depois, long total) {
        return List.of(uf, depois, total == 0 ? 0.0 : 100.0 * depois / total);
    }

    /** Cada percentual é calculado dentro do seu grupo de seções (até / após o limite). */
    static Tabela candidatosAteEDepoisDoFimDaVotacao(ResultadoApuracao boletins) {
        var ate = boletins.somarEstados(ResultadoEstado::votosAteFimDaVotacao);
        var depois = boletins.somarEstados(ResultadoEstado::votosDepoisDoFimDaVotacao);
        var s = sufixoLimite(boletins);

        var linhas = Stream.concat(
                ate.emOrdem().map(e -> List.<Object>of(e.getKey(), e.getValue(), ate.percentual(e.getKey()),
                        depois.de(e.getKey()), depois.percentual(e.getKey()))),
                Stream.of(List.<Object>of("TOTAL", ate.total(), 100.0, depois.total(), 100.0)));

        return new Tabela("candidatos_ate_e_apos_fim_da_votacao_%s.csv".formatted(s),
                List.of("CANDIDATO", "VOTOS_ATE_" + s, "PERCENTUAL_ATE_" + s, "VOTOS_APOS_" + s, "PERCENTUAL_APOS_" + s),
                linhas.toList());
    }

    /** Ex.: "1710" para 17:10, para o nome do arquivo deixar claro qual corte foi usado. */
    private static String sufixoLimite(ResultadoApuracao boletins) {
        return boletins.horario().limite().toString().replace(":", "");
    }
}
