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
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Apuracao {

    /** Horário oficial de encerramento da votação (Brasília). */
    public static final LocalTime FIM_DA_VOTACAO = LocalTime.of(17, 0);

    private final VotoReader leitor;
    private final FusoHorario fusoHorario;
    private final Duration tolerancia;
    private final LocalTime limiteComTolerancia;

    private Apuracao(Builder builder) {
        this.leitor = builder.leitor;
        this.fusoHorario = builder.fusoHorario;
        this.tolerancia = builder.tolerancia;
        this.limiteComTolerancia = FIM_DA_VOTACAO.plus(tolerancia);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public static final Path DIRETORIO_PADRAO = Path.of("bweb");
        /** O TSE considera 17h10 o horário estimado para os procedimentos de encerramento da urna. */
        public static final Duration TOLERANCIA_PADRAO = Duration.ofMinutes(10);

        private VotoReader leitor = new VotoReader(DIRETORIO_PADRAO);
        private FusoHorario fusoHorario = new FusoHorario();
        private Duration tolerancia = TOLERANCIA_PADRAO;

        private Builder() { }

        public Builder diretorio(Path diretorio) {
            this.leitor = new VotoReader(Objects.requireNonNull(diretorio, "diretorio"));
            return this;
        }

        public Builder leitor(VotoReader leitor) {
            this.leitor = Objects.requireNonNull(leitor, "leitor");
            return this;
        }

        public Builder fusoHorario(FusoHorario fusoHorario) {
            this.fusoHorario = Objects.requireNonNull(fusoHorario, "fusoHorario");
            return this;
        }

        /**
         * Tempo dado ao mesário para encerrar a urna depois do fim da votação; seções que
         * fecharam dentro dessa tolerância não contam como votação após o fim.
         */
        public Builder tolerancia(Duration tolerancia) {
            Objects.requireNonNull(tolerancia, "tolerancia");
            if (tolerancia.isNegative()) throw new IllegalArgumentException("tolerância não pode ser negativa: " + tolerancia);
            this.tolerancia = tolerancia;
            return this;
        }

        public Apuracao build() {
            return new Apuracao(this);
        }
    }

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

    private Collector<Voto, ?, Contagem<Candidato>> somarTodosVotosValidos() {
        return Collectors.filtering(
                v -> !v.DS_CARGO_PERGUNTA().equals("Governador") && !v.DS_CARGO_PERGUNTA().equals("#NULO#"),
                Contagem.por(v -> new Candidato(v.DS_CARGO_PERGUNTA(), v.NM_VOTAVEL()), Voto::QT_VOTOS));
    }

    /**
     * O boletim não registra o horário de cada voto, só o de encerramento da seção.
     * Separa os votos para Presidente, por candidato, entre seções que encerraram até o fim da votação
     * com tolerância (false) e depois dele (true), no horário de Brasília.
     */
    private Collector<Voto, ?, Map<Boolean, Contagem<String>>> votosPorCandidatoAntesEDepoisDoFimDaVotacao() {
        return Collectors.filtering(
                v -> v.DS_CARGO_PERGUNTA().equals("Presidente") && v.DT_ENCERRAMENTO() != null,
                Collectors.partitioningBy(
                        v -> fusoHorario.emBrasilia(v, v.DT_ENCERRAMENTO()).toLocalTime().isAfter(limiteComTolerancia),
                        Contagem.por(Voto::NM_VOTAVEL, Voto::QT_VOTOS)));
    }

    private String ufDoArquivo(File csv) {
        return csv.getName().replace(".csv", "");
    }
}
