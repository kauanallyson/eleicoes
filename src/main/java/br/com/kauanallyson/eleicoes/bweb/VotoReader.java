/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Lê os boletins de urna (bweb) de um diretório. */
public class VotoReader {

    private static final char CSV_DELIMITER = ';';

    private final Path diretorio;

    public VotoReader(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** Lista os CSVs do diretório, em ordem alfabética. */
    public List<File> listCsv() throws IOException {
        try (var files = Files.list(diretorio)) {
            return files.filter(file -> !Files.isDirectory(file))
                    .filter(file -> file.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .map(Path::toFile)
                    .toList();
        }
    }

    /**
     * Lê o CSV linha a linha sob demanda. O chamador precisa fechar o Stream
     * (try-with-resources) para liberar o arquivo.
     */
    public Stream<Voto> read(File csv) throws IOException {
        if (!csv.exists()) throw new IllegalArgumentException("Arquivo não encontrado: " + csv);

        var reader = Files.newBufferedReader(csv.toPath(), StandardCharsets.UTF_8);
        var votos = new CsvToBeanBuilder<Voto>(reader)
                .withType(Voto.class)
                .withSeparator(CSV_DELIMITER)
                .build();

        return StreamSupport.stream(votos.spliterator(), false)
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
