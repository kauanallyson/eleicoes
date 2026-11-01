/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;
import br.com.kauanallyson.eleicoes.relatorio.ResultadoCsvWriter;

void main() throws IOException {
    var apuracao = Apuracao.builder().build();
    var csv = new ResultadoCsvWriter(Path.of("resultados"));
    var relatorio = new Relatorio(csv, 5);

    var resultado = apuracao.apurar();
    csv.limparResultadosAnteriores();

    relatorio.votosPorEstado(resultado);
    IO.println();
    relatorio.votosTotais(resultado);
    IO.println();
    relatorio.votosDepoisDoFimDaVotacao(resultado);
    IO.println();
    relatorio.candidatosAteEDepoisDoFimDaVotacao(resultado);
}
