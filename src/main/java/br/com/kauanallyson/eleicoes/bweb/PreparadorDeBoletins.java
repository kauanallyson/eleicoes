/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * Transforma os pacotes do TSE ({@code bweb_2t_AC_261020260742.zip}) nos CSVs que o {@link VotoReader} lê:
 * extrai o CSV, converte de Latin-1 (como o TSE publica) para UTF-8 e grava como {@code AC.csv}.
 */
public class PreparadorDeBoletins {

    private static final Pattern PACOTE = Pattern.compile("bweb_\\dt_([A-Z]{2})_\\d+\\.zip");

    private final Path diretorio;

    public PreparadorDeBoletins(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** @return as UFs preparadas, em ordem alfabética; sobrescreve os {@code UF.csv} que já existirem */
    public List<String> preparar() throws IOException {
        var pacotesPorUf = pacotesPorUf();
        for (var uf : pacotesPorUf.keySet()) {
            var pacotes = pacotesPorUf.get(uf);
            if (pacotes.size() > 1) {
                throw new IllegalStateException("Mais de um pacote para " + uf + " em " + diretorio + ": " + pacotes);
            }
            converter(pacotes.getFirst(), diretorio.resolve(uf + ".csv"));
        }
        return pacotesPorUf.keySet().stream().sorted().toList();
    }

    private Map<String, List<Path>> pacotesPorUf() throws IOException {
        if (!Files.isDirectory(diretorio)) return Map.of();
        try (var arquivos = Files.list(diretorio)) {
            return arquivos.filter(f -> PACOTE.matcher(f.getFileName().toString()).matches())
                    .sorted()
                    .collect(Collectors.groupingBy(PreparadorDeBoletins::ufDoPacote));
        }
    }

    private static String ufDoPacote(Path pacote) {
        var m = PACOTE.matcher(pacote.getFileName().toString());
        if (!m.matches()) throw new IllegalArgumentException("Nome de pacote inesperado: " + pacote);
        return m.group(1);
    }

    /** Grava num arquivo temporário e só então substitui o destino, para não deixar um CSV pela metade. */
    private void converter(Path pacote, Path destino) throws IOException {
        try (var zip = new ZipFile(pacote.toFile())) {
            var csv = zip.stream()
                    .filter(e -> e.getName().endsWith(".csv"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nenhum CSV dentro de " + pacote));

            var temporario = Files.createTempFile(diretorio, destino.getFileName().toString(), ".tmp");
            try (var latin1 = new InputStreamReader(zip.getInputStream(csv), StandardCharsets.ISO_8859_1);
                 var utf8 = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                latin1.transferTo(utf8);
            } catch (IOException e) {
                Files.deleteIfExists(temporario);
                throw e;
            }
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
    }
}
