/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.relatorio;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Grava as tabelas em CSV no mesmo padrão do TSE: separador ';', UTF-8 e decimal com vírgula. */
public class ResultadoCsvWriter {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Path diretorio;

    public ResultadoCsvWriter(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** Apaga os CSVs de execuções anteriores e grava um arquivo por tabela. */
    public void gravar(List<Tabela> tabelas) throws IOException {
        prepararDiretorio();
        for (var tabela : tabelas) {
            gravar(tabela);
        }
    }

    /** Cria o diretório e apaga os CSVs de execuções anteriores; os outros arquivos ficam. */
    public void prepararDiretorio() throws IOException {
        Files.createDirectories(diretorio);
        try (var arquivos = Files.list(diretorio)) {
            for (var csv : arquivos.filter(f -> f.getFileName().toString().endsWith(".csv")).toList()) {
                Files.delete(csv);
            }
        }
    }

    public void gravar(Tabela tabela) throws IOException {
        var linhas = Stream.concat(
                Stream.<String[]>of(tabela.cabecalho().toArray(String[]::new)),
                tabela.linhas().stream().map(linha -> linha.stream().map(this::celula).toArray(String[]::new)));

        try (var writer = new CSVWriter(
                Files.newBufferedWriter(diretorio.resolve(tabela.nome()), StandardCharsets.UTF_8),
                ';', ICSVWriter.DEFAULT_QUOTE_CHARACTER, ICSVWriter.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.DEFAULT_LINE_END)) {
            writer.writeAll(linhas.toList());
        }
    }

    private String celula(Object valor) {
        return switch (valor) {
            case Percentual percentual -> String.format(PT_BR, "%.2f", percentual.valor());
            case Boolean sim -> sim ? "SIM" : "NAO";
            default -> String.valueOf(valor);
        };
    }
}
