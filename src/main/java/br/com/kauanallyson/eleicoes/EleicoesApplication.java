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
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Uso: {@code [--ano=2026] [--turno=1|2] [--relatorios=nome,nome...] [--sim]}; veja {@link Params}.
 *
 * <p>Erros esperados (argumento inválido, falta de rede, arquivo ausente) saem como uma mensagem curta em
 * stderr, sem stack trace; só um bug de verdade mostra o stack.
 */
public class EleicoesApplication {

    static final int OK = 0;
    static final int FALHOU = 1;
    static final int USO_INVALIDO = 2;

    public static void main(String[] args) {
        int codigo = executar(args, System.err);
        if (codigo != OK) System.exit(codigo);
    }

    static int executar(String[] args, PrintStream erro) {
        Params params;
        try {
            params = Params.parse(args);
        } catch (IllegalArgumentException e) {
            erro.println("erro: " + e.getMessage());
            return USO_INVALIDO;
        }

        try {
            return apurar(params) ? OK : FALHOU;
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            erro.println("erro: " + mensagem(e));
            return FALHOU;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            erro.println("erro: interrompido");
            return FALHOU;
        }
    }

    /** @return {@code false} se faltavam boletins e o usuário não autorizou o download */
    private static boolean apurar(Params params) throws IOException, InterruptedException {
        var eleicao = params.eleicao();
        IO.println("[eleição] " + eleicao);

        var boletins = Path.of("bweb").resolve(eleicao.pasta());
        var resultados = Path.of("resultados").resolve(eleicao.pasta());
        var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        var portal = PortalTseHttp.boletins(http, eleicao, IO::println);
        var download = new DownloadDeBoletins(boletins, portal, TerminalConfirmation.of(params.autoConfirm()), IO::println);
        if (!download.garantir()) return false;

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
        return true;
    }

    /**
     * A primeira mensagem da cadeia de causas, pulando o {@link UncheckedIOException} (que só embrulha a causa).
     * As exceções de rede do {@link HttpClient} (ex.: {@code ConnectException}) costumam vir sem mensagem.
     */
    static String mensagem(Throwable e) {
        for (var t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && !(t instanceof UncheckedIOException)) return t.getMessage();
        }
        return "falha de rede ou de arquivo (" + e.getClass().getSimpleName() + ")";
    }
}
