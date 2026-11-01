/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.Parametros;
import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.apuracao.HorarioDaVotacao;
import br.com.kauanallyson.eleicoes.bweb.CatalogoTse;
import br.com.kauanallyson.eleicoes.bweb.Confirmacao;
import br.com.kauanallyson.eleicoes.bweb.DownloadDeBoletins;
import br.com.kauanallyson.eleicoes.bweb.VotoReader;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

import java.net.http.HttpClient;

/** Uso: {@code [--ano=2026] [--turno=1|2] [--sim]}. */
void main(String[] args) throws IOException, InterruptedException {
    var parametros = Parametros.de(args);
    var eleicao = parametros.eleicao();
    IO.println("[eleição] " + eleicao);

    var boletins = Path.of("bweb").resolve(eleicao.pasta());
    var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    var download = new DownloadDeBoletins(
            boletins, CatalogoTse.boletins(http, eleicao), Confirmacao.doTerminal(parametros.sim()), http, IO::println);
    if (!download.garantir()) return;

    var horario = HorarioDaVotacao.da(eleicao, Duration.ofMinutes(10));

    var apuracao = Apuracao.builder()
            .fonte(new VotoReader(boletins))
            .horario(horario)
            .cargo(eleicao.cargoPrincipal())
            .build();

    var relatorio = Relatorio.builder()
            .diretorio(Path.of("resultados").resolve(eleicao.pasta()))
            .build();

    relatorio.gravar(apuracao.apurar());
}
