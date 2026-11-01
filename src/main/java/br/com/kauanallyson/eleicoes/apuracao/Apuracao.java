/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Apuracao {

    private final FonteDeVotos<VotosNaSecao> fonte;
    private final HorarioDaVotacao horario;

    private Apuracao(Builder builder) {
        this.fonte = builder.fonte;
        this.horario = builder.horario;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private FonteDeVotos<VotosNaSecao> fonte;
        private HorarioDaVotacao horario = HorarioDaVotacao.padrao();

        private Builder() { }

        public Builder fonte(FonteDeVotos<VotosNaSecao> fonte) {
            this.fonte = Objects.requireNonNull(fonte, "fonte");
            return this;
        }

        public Builder horario(HorarioDaVotacao horario) {
            this.horario = Objects.requireNonNull(horario, "horario");
            return this;
        }

        public Apuracao build() {
            Objects.requireNonNull(fonte, "fonte");
            return new Apuracao(this);
        }
    }

    public ResultadoApuracao apurar() throws IOException {
        var estados = fonte.ufs().parallelStream()
                .map(this::apurarEstado)
                .toList();
        return new ResultadoApuracao(estados, horario);
    }

    /** Lê o arquivo uma única vez e calcula as duas contas na mesma passada. */
    private ResultadoEstado apurarEstado(String uf) {
        try (var votos = fonte.registros(uf)) {
            return votos.collect(Collectors.teeing(
                    somarTodosVotosValidos(),
                    votosPorCandidatoAntesEDepoisDoFimDaVotacao(),
                    (validos, porFim) -> new ResultadoEstado(uf, validos, porFim.get(false), porFim.get(true))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Collector<VotosNaSecao, ?, Contagem<Candidato>> somarTodosVotosValidos() {
        return Collectors.filtering(
                v -> !v.cargo().equals("Governador") && !v.cargo().equals("#NULO#"),
                Contagem.por(v -> new Candidato(v.cargo(), v.votavel()), VotosNaSecao::quantidade));
    }

    /**
     * O boletim não registra o horário de cada voto, só o de encerramento da seção.
     * Separa os votos para Presidente, por candidato, entre seções que encerraram até o limite
     * (false) e depois dele (true).
     */
    private Collector<VotosNaSecao, ?, Map<Boolean, Contagem<String>>> votosPorCandidatoAntesEDepoisDoFimDaVotacao() {
        return Collectors.filtering(
                v -> v.cargo().equals("Presidente") && v.encerramentoLocal() != null,
                Collectors.partitioningBy(
                        v -> horario.encerrouDepoisDoLimite(v.uf(), v.municipio(), v.encerramentoLocal()),
                        Contagem.por(VotosNaSecao::votavel, VotosNaSecao::quantidade)));
    }

}
