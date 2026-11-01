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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Apuracao {

    private final VotoReader leitor;
    private final HorarioDaVotacao horario;

    private Apuracao(Builder builder) {
        this.leitor = builder.leitor;
        this.horario = builder.horario;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public static final Path DIRETORIO_PADRAO = Path.of("bweb");

        private VotoReader leitor = new VotoReader(DIRETORIO_PADRAO);
        private HorarioDaVotacao horario = HorarioDaVotacao.padrao();

        private Builder() { }

        public Builder diretorio(Path diretorio) {
            this.leitor = new VotoReader(Objects.requireNonNull(diretorio, "diretorio"));
            return this;
        }

        public Builder leitor(VotoReader leitor) {
            this.leitor = Objects.requireNonNull(leitor, "leitor");
            return this;
        }

        public Builder horario(HorarioDaVotacao horario) {
            this.horario = Objects.requireNonNull(horario, "horario");
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
        return new ResultadoApuracao(estados, horario);
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
     * Separa os votos para Presidente, por candidato, entre seções que encerraram até o limite
     * (false) e depois dele (true).
     */
    private Collector<Voto, ?, Map<Boolean, Contagem<String>>> votosPorCandidatoAntesEDepoisDoFimDaVotacao() {
        return Collectors.filtering(
                v -> v.DS_CARGO_PERGUNTA().equals("Presidente") && v.DT_ENCERRAMENTO() != null,
                Collectors.partitioningBy(
                        v -> horario.encerrouDepoisDoLimite(v.SG_UF(), v.CD_MUNICIPIO(), v.DT_ENCERRAMENTO()),
                        Contagem.por(Voto::NM_VOTAVEL, Voto::QT_VOTOS)));
    }

    private String ufDoArquivo(File csv) {
        return csv.getName().replace(".csv", "");
    }
}
