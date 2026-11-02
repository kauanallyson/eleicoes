/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;

import java.time.LocalDateTime;

/**
 * Uma linha do boletim de urna (bweb), só com as colunas que a apuração usa; o OpenCSV ignora as demais
 * colunas do CSV. Os nomes dos campos são os do cabeçalho do TSE. É uma classe (e não um record) porque o
 * OpenCSV precisa de um construtor sem argumentos.
 */
public class Voto {

    @CsvBindByName(column = "SG_UF")
    private String SG_UF;

    @CsvBindByName(column = "CD_MUNICIPIO")
    private int CD_MUNICIPIO;

    @CsvBindByName(column = "DS_CARGO_PERGUNTA")
    private String DS_CARGO_PERGUNTA;

    @CsvBindByName(column = "NM_VOTAVEL")
    private String NM_VOTAVEL;

    @CsvBindByName(column = "QT_VOTOS")
    private int QT_VOTOS;

    /** Vazio nas linhas sem seção (ex.: juntas apuradoras do exterior). */
    @CsvBindByName(column = "DT_ENCERRAMENTO")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_ENCERRAMENTO;

    public String SG_UF() { return SG_UF; }
    public int CD_MUNICIPIO() { return CD_MUNICIPIO; }
    public String DS_CARGO_PERGUNTA() { return DS_CARGO_PERGUNTA; }
    public String NM_VOTAVEL() { return NM_VOTAVEL; }
    public int QT_VOTOS() { return QT_VOTOS; }
    public LocalDateTime DT_ENCERRAMENTO() { return DT_ENCERRAMENTO; }
}
