/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes;

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.bweb.DownloadDeBoletins;
import br.com.kauanallyson.eleicoes.bweb.PortalTseHttp;
import br.com.kauanallyson.eleicoes.bweb.VotoReader;
import br.com.kauanallyson.eleicoes.cli.Params;
import br.com.kauanallyson.eleicoes.cli.TerminalConfirmation;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Uso: {@code [--ano=2026] [--turno=1|2] [--relatorios=nome,nome...] [--sim]}; veja {@link Params}.
 *
 * <p>Erros esperados são capturados e logados numa linha, sem stack trace, e definem o {@link CodigoDeSaida}.
 */
public class EleicoesApplication {

    public static void main(String[] args) {
        try {
            var params = Params.parse(args);
            var eleicao = params.eleicao();
            IO.println("[eleição] " + eleicao);

            var boletins = Path.of("bweb").resolve(eleicao.pasta());
            var resultados = Path.of("resultados").resolve(eleicao.pasta());
            var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

            var portal = PortalTseHttp.boletins(http, eleicao, IO::println);
            var download = new DownloadDeBoletins(boletins, portal, TerminalConfirmation.of(params.autoConfirm()), IO::println);
            if (!download.garantir()) CodigoDeSaida.FALHOU.sair();

            var apuracao = Apuracao.builder()
                    .boletins(new VotoReader(boletins))
                    .eleicao(eleicao)
                    .tolerancia(Duration.ofMinutes(10))
                    .build();

            var relatorio = Relatorio.builder()
                    .diretorio(resultados)
                    .tipos(params.relatorios())
                    .build();

            relatorio.gravar(apuracao.apurar());
        } catch (IllegalArgumentException e) {
            erro(e);
            CodigoDeSaida.USO_INVALIDO.sair();
        } catch (IOException | UncheckedIOException e) {
            erro(e);
            CodigoDeSaida.FALHOU.sair();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            erro(e);
            CodigoDeSaida.FALHOU.sair();
        }
    }

    private static void erro(Exception e) {
        System.err.println("[erro] " + (e.getMessage() != null ? e.getMessage() : e));
    }
}
