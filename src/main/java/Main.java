/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.apuracao.HorarioDaVotacao;
import br.com.kauanallyson.eleicoes.bweb.CatalogoTse;
import br.com.kauanallyson.eleicoes.bweb.Confirmacao;
import br.com.kauanallyson.eleicoes.bweb.DownloadDeBoletins;
import br.com.kauanallyson.eleicoes.bweb.VotoReader;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

import java.net.http.HttpClient;

void main(String[] args) throws IOException, InterruptedException {
    var boletins = Path.of("bweb");
    var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    var download = new DownloadDeBoletins(
            boletins, CatalogoTse.segundoTurno2026(http), Confirmacao.doTerminal(args), http, IO::println);
    if (!download.garantir()) return;

    var horario = new HorarioDaVotacao(LocalDate.of(2026, 10, 25), Duration.ofMinutes(10));

    var apuracao = Apuracao.builder()
            .fonte(new VotoReader(boletins))
            .horario(horario)
            .build();

    var relatorio = Relatorio.builder()
            .diretorio(Path.of("resultados"))
            .build();

    relatorio.gravar(apuracao.apurar());
}
