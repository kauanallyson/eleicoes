/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Grava as respostas em CSV no mesmo padrão do TSE: separador ';', UTF-8 e decimal com vírgula. */
public class ResultadoCsvWriter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Path diretorio;

    public ResultadoCsvWriter(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** Apaga os CSVs de execuções anteriores, para o diretório ter só o resultado atual. */
    public void limparResultadosAnteriores() throws IOException {
        if (!Files.isDirectory(diretorio)) return;
        try (var arquivos = Files.list(diretorio)) {
            for (var csv : arquivos.filter(f -> f.getFileName().toString().endsWith(".csv")).toList()) {
                Files.delete(csv);
            }
        }
    }

    void write(String arquivo, List<String[]> linhas) {
        try {
            Files.createDirectories(diretorio);
            try (var writer = new CSVWriter(
                    Files.newBufferedWriter(caminho(arquivo), StandardCharsets.UTF_8),
                    ';', ICSVWriter.DEFAULT_QUOTE_CHARACTER, ICSVWriter.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.DEFAULT_LINE_END)) {
                writer.writeAll(linhas);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path caminho(String arquivo) {
        return diretorio.resolve(arquivo);
    }

    String percentual(double valor) {
        return String.format(PT_BR, "%.2f", valor);
    }
}
