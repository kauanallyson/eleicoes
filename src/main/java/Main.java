/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.apuracao.HorarioDaVotacao;
import br.com.kauanallyson.eleicoes.bweb.PreparadorDeBoletins;
import br.com.kauanallyson.eleicoes.bweb.VotoReader;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

void main() throws IOException {
    var boletins = Path.of("bweb");
    if ("true".equalsIgnoreCase(System.getenv("ELEICOES_PREPARAR_BOLETINS"))) {
        new PreparadorDeBoletins(boletins).preparar();
    }

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
