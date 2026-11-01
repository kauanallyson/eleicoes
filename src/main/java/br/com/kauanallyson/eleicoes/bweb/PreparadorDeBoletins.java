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
import java.util.zip.ZipFile;

/**
 * Transforma um pacote do TSE ({@code bweb_2t_AC_261020260742.zip}) no CSV que o {@link VotoReader} lê:
 * extrai o CSV, converte de Latin-1 (como o TSE publica) para UTF-8 e grava como {@code AC.csv}.
 */
class PreparadorDeBoletins {

    private final Path diretorio;

    PreparadorDeBoletins(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** Grava num arquivo temporário e só então substitui {@code UF.csv}, para não deixar um CSV pela metade. */
    void preparar(Path pacote, String uf) throws IOException {
        var destino = diretorio.resolve(uf + ".csv");
        try (var zip = new ZipFile(pacote.toFile())) {
            var csv = zip.stream()
                    .filter(e -> e.getName().endsWith(".csv"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Nenhum CSV dentro de " + pacote));

            var temporario = Files.createTempFile(diretorio, uf, ".tmp");
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
