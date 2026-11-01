/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.apuracao.FusoHorario;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

void main() throws IOException {
    var apuracao = Apuracao.builder()
            .diretorio(Path.of("bweb"))
            .fusoHorario(new FusoHorario())
            .tolerancia(Duration.ofMinutes(10))
            .build();

    var relatorio = Relatorio.builder()
            .diretorio(Path.of("resultados"))
            .top(5)
            .build();

    var resultado = apuracao.apurar();
    relatorio.limparResultadosAnteriores();

    relatorio.votosPorEstado(resultado);
    IO.println();
    relatorio.votosTotais(resultado);
    IO.println();
    relatorio.votosDepoisDoFimDaVotacao(resultado);
    IO.println();
    relatorio.candidatosAteEDepoisDoFimDaVotacao(resultado);
}
