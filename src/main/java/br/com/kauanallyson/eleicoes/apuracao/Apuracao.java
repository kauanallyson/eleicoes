/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** Apura os votos para Presidente por UF a partir dos boletins de urna. */
public class Apuracao {

    /** O único cargo apurado; as linhas de outros cargos do boletim são descartadas. */
    static final String PRESIDENTE = "Presidente";

    private final FonteDeVotos<VotosNaSecao> boletins;
    private final HorarioDaVotacao horario;
    private final Consumer<String> log;

    private Apuracao(Builder builder) {
        this.boletins = builder.boletins;
        this.horario = HorarioDaVotacao.da(builder.eleicao, builder.tolerancia);
        this.log = builder.log;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private FonteDeVotos<VotosNaSecao> boletins;
        private Eleicao eleicao;
        private Duration tolerancia = HorarioDaVotacao.TOLERANCIA_PADRAO;
        private Consumer<String> log = mensagem -> { };

        private Builder() { }

        public Builder boletins(FonteDeVotos<VotosNaSecao> boletins) {
            this.boletins = Objects.requireNonNull(boletins, "boletins");
            return this;
        }

        /** Define o dia da votação. */
        public Builder eleicao(Eleicao eleicao) {
            this.eleicao = Objects.requireNonNull(eleicao, "eleicao");
            return this;
        }

        /** @see HorarioDaVotacao#HorarioDaVotacao(java.time.LocalDate, Duration) */
        public Builder tolerancia(Duration tolerancia) {
            this.tolerancia = Objects.requireNonNull(tolerancia, "tolerancia");
            return this;
        }

        /** Opcional: recebe o andamento, uma linha por UF apurada, com o percentual concluído. */
        public Builder log(Consumer<String> log) {
            this.log = Objects.requireNonNull(log, "log");
            return this;
        }

        public Apuracao build() {
            Objects.requireNonNull(boletins, "boletins");
            Objects.requireNonNull(eleicao, "eleicao");
            return new Apuracao(this);
        }
    }

    /** Apura as UFs em paralelo; toList() mantém a ordem das UFs. */
    public ResultadoApuracao apurar() throws IOException {
        var ufs = boletins.ufs();
        var prontas = new AtomicInteger();
        var estados = ufs.parallelStream()
                .map(uf -> {
                    var estado = apurarEstado(uf);
                    andamento(prontas, ufs.size(), uf);
                    return estado;
                })
                .toList();
        return new ResultadoApuracao(estados, horario);
    }

    /** Sincronizado para o percentual sair sempre crescente, mesmo com as UFs terminando em paralelo. */
    private void andamento(AtomicInteger prontas, int total, String uf) {
        synchronized (prontas) {
            int n = prontas.incrementAndGet();
            log.accept("[apuração] %3d%% (%d/%d UFs) %s".formatted(100 * n / total, n, total, uf));
        }
    }

    private ResultadoEstado apurarEstado(String uf) {
        try (var votos = boletins.registros(uf)) {
            return votos.collect(Collectors.teeing(
                    somarVotosParaPresidente(),
                    votosAntesEDepoisDoLimite(),
                    (validos, porFim) -> new ResultadoEstado(uf, validos, porFim.get(false), porFim.get(true))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Collector<VotosNaSecao, ?, Contagem<String>> somarVotosParaPresidente() {
        return Collectors.filtering(
                v -> v.cargo().equals(PRESIDENTE),
                Contagem.por(VotosNaSecao::votavel, VotosNaSecao::quantidade));
    }

    /**
     * O boletim não registra o horário de cada voto, só o de encerramento da seção.
     * Separa os votos para Presidente, por candidato, entre seções que encerraram até o limite (false) e depois (true).
     */
    private Collector<VotosNaSecao, ?, Map<Boolean, Contagem<String>>> votosAntesEDepoisDoLimite() {
        return Collectors.filtering(
                v -> v.cargo().equals(PRESIDENTE) && v.encerramentoLocal() != null,
                Collectors.partitioningBy(
                        v -> horario.encerrouDepoisDoLimite(v.uf(), v.municipio(), v.encerramentoLocal()),
                        Contagem.por(VotosNaSecao::votavel, VotosNaSecao::quantidade)));
    }
}
