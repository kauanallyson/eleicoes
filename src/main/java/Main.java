/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

void main() throws IOException {
    var apuracao = Apuracao.builder().build();
    var relatorio = Relatorio.builder().build();

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
