/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import br.com.kauanallyson.eleicoes.bweb.Voto;
import br.com.kauanallyson.eleicoes.bweb.VotoReader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** Contagem dos votos a partir dos CSVs do boletim de urna. */
public class Apuracao {

    /** Horário oficial de encerramento da votação (Brasília). */
    public static final LocalTime FIM_DA_VOTACAO = LocalTime.of(17, 0);

    private final VotoReader leitor;
    private final FusoHorario fusoHorario;
    private final Duration tolerancia;
    private final LocalTime limiteComTolerancia;

    /**
     * @param tolerancia tempo dado ao mesário para encerrar a urna depois do fim da votação; seções que
     *                   fecharam dentro dessa tolerância não contam como votação após o fim
     */
    public Apuracao(VotoReader leitor, FusoHorario fusoHorario, Duration tolerancia) {
        this.leitor = leitor;
        this.fusoHorario = fusoHorario;
        this.tolerancia = tolerancia;
        this.limiteComTolerancia = FIM_DA_VOTACAO.plus(tolerancia);
    }

    /** Apura cada arquivo em paralelo (ForkJoinPool comum); toList() mantém a ordem dos arquivos. */
    public ResultadoApuracao apurar() throws IOException {
        var estados = leitor.listCsv().parallelStream()
                .map(this::apurarEstado)
                .toList();
        return new ResultadoApuracao(estados, FIM_DA_VOTACAO, tolerancia);
    }

    /** Lê o arquivo uma única vez e calcula as duas contas na mesma passada. */
    private ResultadoEstado apurarEstado(File csv) {
        try (var votos = leitor.read(csv)) {
            return votos.collect(Collectors.teeing(
                    somarTodosVotosValidos(),
                    votosPorCandidatoAntesEDepoisDoFimDaVotacao(),
                    (validos, porFim) -> new ResultadoEstado(ufDoArquivo(csv), validos, porFim.get(false), porFim.get(true))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Collector<Voto, ?, Map<Candidato, Long>> somarTodosVotosValidos() {
        return Collectors.filtering(
                v -> !v.DS_CARGO_PERGUNTA().equals("Governador") && !v.DS_CARGO_PERGUNTA().equals("#NULO#"),
                Collectors.groupingBy(
                        v -> new Candidato(v.DS_CARGO_PERGUNTA(), v.NM_VOTAVEL()),
                        Collectors.summingLong(Voto::QT_VOTOS)));
    }

    /**
     * O boletim não registra o horário de cada voto, só o de encerramento da seção.
     * Separa os votos para Presidente, por candidato, entre seções que encerraram até o fim da votação
     * com tolerância (false) e depois dele (true), no horário de Brasília.
     */
    private Collector<Voto, ?, Map<Boolean, Map<String, Long>>> votosPorCandidatoAntesEDepoisDoFimDaVotacao() {
        return Collectors.filtering(
                v -> v.DS_CARGO_PERGUNTA().equals("Presidente") && v.DT_ENCERRAMENTO() != null,
                Collectors.partitioningBy(
                        v -> fusoHorario.emBrasilia(v, v.DT_ENCERRAMENTO()).toLocalTime().isAfter(limiteComTolerancia),
                        Collectors.groupingBy(Voto::NM_VOTAVEL, Collectors.summingLong(Voto::QT_VOTOS))));
    }

    private String ufDoArquivo(File csv) {
        return csv.getName().replace(".csv", "");
    }
}
