/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.net.URI;

/** Um pacote de boletins de urna publicado pelo TSE, com o hash SHA-512 que o acompanha. */
public record PacoteTse(String uf, URI zip, URI sha512, long tamanho) {

    public static final long TAMANHO_DESCONHECIDO = -1;

    public PacoteTse comTamanho(long bytes) {
        return new PacoteTse(uf, zip, sha512, bytes);
    }

    public String nomeDoArquivo() {
        var caminho = zip.getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }
}
