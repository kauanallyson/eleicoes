/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import com.opencsv.bean.CsvBindByName;

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

    @CsvBindByName(column = "NR_ZONA", required = true)
    private int NR_ZONA;

    @CsvBindByName(column = "NR_SECAO", required = true)
    private int NR_SECAO;

    @CsvBindByName(column = "DS_CARGO_PERGUNTA")
    private String DS_CARGO_PERGUNTA;

    @CsvBindByName(column = "NM_VOTAVEL")
    private String NM_VOTAVEL;

    @CsvBindByName(column = "QT_VOTOS")
    private int QT_VOTOS;

    @CsvBindByName(column = "QT_APTOS", required = true)
    private int QT_APTOS;

    @CsvBindByName(column = "QT_COMPARECIMENTO", required = true)
    private int QT_COMPARECIMENTO;

    /** Vazio nas linhas sem seção (ex.: juntas apuradoras do exterior). */
    /** Texto: o formato da data muda de ano para ano, e quem converte é o {@link VotoReader}. */
    @CsvBindByName(column = "DT_ENCERRAMENTO")
    private String DT_ENCERRAMENTO;

    public String SG_UF() { return SG_UF; }
    public int CD_MUNICIPIO() { return CD_MUNICIPIO; }
    public int NR_ZONA() { return NR_ZONA; }
    public int NR_SECAO() { return NR_SECAO; }
    public String DS_CARGO_PERGUNTA() { return DS_CARGO_PERGUNTA; }
    public String NM_VOTAVEL() { return NM_VOTAVEL; }
    public int QT_VOTOS() { return QT_VOTOS; }
    public int QT_APTOS() { return QT_APTOS; }
    public int QT_COMPARECIMENTO() { return QT_COMPARECIMENTO; }
    public String DT_ENCERRAMENTO() { return DT_ENCERRAMENTO; }
}
