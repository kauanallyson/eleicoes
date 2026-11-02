/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Os pacotes de boletim de urna publicados pelo TSE para uma eleição (HTTP em produção, memória nos testes). */
public interface PortalTse {

    /** Um pacote por UF, sem o tamanho. */
    List<PacoteTse> pacotes() throws IOException, InterruptedException;

    /** O pacote com o tamanho do download preenchido. */
    PacoteTse comTamanho(PacoteTse pacote) throws IOException, InterruptedException;

    /**
     * Baixa o pacote para {@code destino} e confere o SHA-512 publicado pelo TSE. Se o hash não conferir, apaga o
     * arquivo e lança {@link IOException}.
     */
    void baixar(PacoteTse pacote, Path destino) throws IOException, InterruptedException;
}
