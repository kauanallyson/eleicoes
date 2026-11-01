/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.net.URI;

/** Um pacote de boletins de urna publicado pelo TSE, com o hash SHA-512 que o acompanha. */
record PacoteTse(String uf, URI zip, URI sha512, long tamanho) {

    String nomeDoArquivo() {
        var caminho = zip.getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }
}
