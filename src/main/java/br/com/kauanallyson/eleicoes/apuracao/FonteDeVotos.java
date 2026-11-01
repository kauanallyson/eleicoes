/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

/** De onde vêm os registros de votação de cada UF (arquivos do TSE em produção, memória nos testes). */
public interface FonteDeVotos<T> {

    /** UFs disponíveis, em ordem alfabética. */
    List<String> ufs() throws IOException;

    /** Registros da UF, lidos sob demanda; o chamador precisa fechar o Stream. */
    Stream<T> registros(String uf) throws IOException;
}
