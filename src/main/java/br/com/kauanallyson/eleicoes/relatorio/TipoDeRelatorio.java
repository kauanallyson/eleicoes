/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import br.com.kauanallyson.eleicoes.apuracao.ResultadoApuracao;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O catálogo de relatórios: cada um tem o nome usado em {@code --relatorios}, uma descrição e a função que
 * monta a sua {@link Tabela}. Um relatório novo é uma constante aqui; a linha de comando e o CSV saem sozinhos.
 */
public enum TipoDeRelatorio {

    POR_ESTADO("por-estado", "votos por candidato em cada UF, com percentual",
            Respostas::votosPorEstado),
    TOTAIS("totais", "votos totais por candidato",
            Respostas::votosTotais),
    DEPOIS_DO_FIM("depois-do-fim", "votos em seções encerradas depois do fim da votação, por UF",
            Respostas::votosDepoisDoFimDaVotacao),
    ATE_E_APOS_O_FIM("ate-e-apos-o-fim", "votos por candidato até e após o fim da votação",
            Respostas::candidatosAteEDepoisDoFimDaVotacao),
    COMPARECIMENTO("comparecimento", "eleitores aptos, comparecimento e abstenção por UF, com percentual",
            Respostas::comparecimentoPorEstado);

    private final String nome;
    private final String descricao;
    private final Function<ResultadoApuracao, Tabela> gerador;

    TipoDeRelatorio(String nome, String descricao, Function<ResultadoApuracao, Tabela> gerador) {
        this.nome = nome;
        this.descricao = descricao;
        this.gerador = gerador;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public Tabela gerar(ResultadoApuracao resultado) {
        return gerador.apply(resultado);
    }

    /** @throws IllegalArgumentException com a lista dos relatórios disponíveis, se o nome não existir */
    public static TipoDeRelatorio porNome(String nome) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.nome.equals(nome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("relatório desconhecido: " + nome + "\n" + catalogo()));
    }

    /** Uma linha por relatório, com nome e descrição, para a mensagem de uso. */
    public static String catalogo() {
        return Arrays.stream(values())
                .map(tipo -> "  %-18s %s".formatted(tipo.nome, tipo.descricao))
                .collect(Collectors.joining("\n", "relatórios disponíveis:\n", ""));
    }
}
