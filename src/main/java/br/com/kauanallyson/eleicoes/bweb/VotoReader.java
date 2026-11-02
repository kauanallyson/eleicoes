/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import br.com.kauanallyson.eleicoes.apuracao.FonteDeVotos;
import br.com.kauanallyson.eleicoes.apuracao.VotosNaSecao;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Boletins de urna do TSE: um CSV por UF ({@code AC.csv}, ..., {@code ZZ.csv}), em UTF-8 e separados por ';'. */
public class VotoReader implements FonteDeVotos<VotosNaSecao> {

    private static final char CSV_DELIMITER = ';';
    private static final String EXTENSAO = ".csv";

    private final Path diretorio;

    public VotoReader(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** A UF é o nome do arquivo. */
    @Override
    public List<String> ufs() throws IOException {
        try (var arquivos = Files.list(diretorio)) {
            return arquivos.filter(Files::isRegularFile)
                    .map(f -> f.getFileName().toString())
                    .filter(nome -> nome.endsWith(EXTENSAO))
                    .map(nome -> nome.substring(0, nome.length() - EXTENSAO.length()))
                    .sorted()
                    .toList();
        }
    }

    @Override
    public Stream<VotosNaSecao> registros(String uf) throws IOException {
        var csv = diretorio.resolve(uf + EXTENSAO);
        if (!Files.exists(csv)) throw new NoSuchFileException(csv.toString(), null, "boletim de urna não encontrado");

        var reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
        var votos = new CsvToBeanBuilder<Voto>(reader)
                .withType(Voto.class)
                .withSeparator(CSV_DELIMITER)
                .build();

        return StreamSupport.stream(votos.spliterator(), false)
                .map(VotoReader::paraDominio)
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private static VotosNaSecao paraDominio(Voto v) {
        return new VotosNaSecao(v.SG_UF(), v.CD_MUNICIPIO(), v.DS_CARGO_PERGUNTA(), v.NM_VOTAVEL(), v.QT_VOTOS(),
                v.DT_ENCERRAMENTO());
    }
}
