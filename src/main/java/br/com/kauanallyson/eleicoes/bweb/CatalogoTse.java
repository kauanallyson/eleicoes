/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import br.com.kauanallyson.eleicoes.apuracao.Eleicao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Descobre os pacotes de boletim de urna de um turno pela API do Portal de Dados Abertos do TSE (CKAN),
 * em vez de deixar as URLs fixas no código. A API não informa o tamanho; ele vem de um HEAD em cada pacote,
 * feito só para os pacotes que vão ser baixados.
 */
public class CatalogoTse {

    private final HttpClient http;
    private final URI api;
    private final int turno;

    public CatalogoTse(HttpClient http, URI api, int turno) {
        this.http = http;
        this.api = api;
        this.turno = turno;
    }

    /** Conjunto "Resultados &lt;ano&gt; - Boletim de Urna" do Portal de Dados Abertos do TSE. */
    public static CatalogoTse boletins(HttpClient http, Eleicao eleicao) {
        var api = URI.create("https://dadosabertos.tse.jus.br/api/3/action/package_show?id=resultados-"
                + eleicao.ano() + "-boletim-de-urna");
        return new CatalogoTse(http, api, eleicao.turno());
    }

    List<PacoteTse> pacotes() throws IOException, InterruptedException {
        var resposta = http.send(HttpRequest.newBuilder(api).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() != 200) {
            throw new IOException("Catálogo do TSE respondeu " + resposta.statusCode() + ": " + api);
        }

        var url = Pattern.compile("\"url\"\\s*:\\s*\"(https?://[^\"]+/(?i:bweb)_" + turno + "t_([A-Z]{2})_\\d+\\.zip)\"");
        var pacotes = new ArrayList<PacoteTse>();
        var m = url.matcher(resposta.body());
        while (m.find()) {
            var zip = URI.create(m.group(1));
            pacotes.add(new PacoteTse(m.group(2), zip, URI.create(m.group(1) + ".sha512"), PacoteTse.TAMANHO_DESCONHECIDO));
        }
        if (pacotes.isEmpty()) throw new IOException("Nenhum pacote do " + turno + "º turno no catálogo do TSE: " + api);
        pacotes.sort(Comparator.comparing(PacoteTse::uf));
        return pacotes;
    }

    PacoteTse comTamanho(PacoteTse pacote) throws IOException, InterruptedException {
        var head = HttpRequest.newBuilder(pacote.zip()).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        return pacote.comTamanho(http.send(head, HttpResponse.BodyHandlers.discarding()).headers()
                .firstValueAsLong("content-length").orElse(PacoteTse.TAMANHO_DESCONHECIDO));
    }
}
