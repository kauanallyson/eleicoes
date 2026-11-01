/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

import br.com.kauanallyson.eleicoes.apuracao.Apuracao;
import br.com.kauanallyson.eleicoes.apuracao.HorarioDaVotacao;
import br.com.kauanallyson.eleicoes.relatorio.Relatorio;

void main() throws IOException {
    var horario = new HorarioDaVotacao(LocalDate.of(2026, 10, 25), Duration.ofMinutes(10));

    var apuracao = Apuracao.builder()
            .diretorio(Path.of("bweb"))
            .horario(horario)
            .build();

    var relatorio = Relatorio.builder()
            .diretorio(Path.of("resultados"))
            .build();

    var resultado = apuracao.apurar();
    relatorio.limparResultadosAnteriores();

    relatorio.votosPorEstado(resultado);
    relatorio.votosTotais(resultado);
    relatorio.votosDepoisDoFimDaVotacao(resultado);
    relatorio.candidatosAteEDepoisDoFimDaVotacao(resultado);
}
