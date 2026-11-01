/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.bweb;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Uma linha do boletim de urna (bweb). Os nomes dos campos são os do cabeçalho do CSV.
 * É uma classe (e não um record) porque o OpenCSV precisa de um construtor sem argumentos.
 */
public class Voto {

    @CsvBindByName(column = "DT_GERACAO")
    @CsvDate("dd/MM/yyyy")
    private LocalDate DT_GERACAO;

    @CsvBindByName(column = "HH_GERACAO")
    @CsvDate("HH:mm:ss")
    private LocalTime HH_GERACAO;

    @CsvBindByName(column = "ANO_ELEICAO")
    private int ANO_ELEICAO;

    @CsvBindByName(column = "CD_TIPO_ELEICAO")
    private int CD_TIPO_ELEICAO;

    @CsvBindByName(column = "NM_TIPO_ELEICAO")
    private String NM_TIPO_ELEICAO;

    @CsvBindByName(column = "CD_PLEITO")
    private int CD_PLEITO;

    @CsvBindByName(column = "DT_PLEITO")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_PLEITO;

    @CsvBindByName(column = "NR_TURNO")
    private int NR_TURNO;

    @CsvBindByName(column = "CD_ELEICAO")
    private int CD_ELEICAO;

    @CsvBindByName(column = "DS_ELEICAO")
    private String DS_ELEICAO;

    @CsvBindByName(column = "SG_UF")
    private String SG_UF;

    @CsvBindByName(column = "CD_MUNICIPIO")
    private int CD_MUNICIPIO;

    @CsvBindByName(column = "NM_MUNICIPIO")
    private String NM_MUNICIPIO;

    @CsvBindByName(column = "NR_ZONA")
    private int NR_ZONA;

    @CsvBindByName(column = "NR_SECAO")
    private int NR_SECAO;

    @CsvBindByName(column = "NR_LOCAL_VOTACAO")
    private int NR_LOCAL_VOTACAO;

    @CsvBindByName(column = "CD_CARGO_PERGUNTA")
    private int CD_CARGO_PERGUNTA;

    @CsvBindByName(column = "DS_CARGO_PERGUNTA")
    private String DS_CARGO_PERGUNTA;

    @CsvBindByName(column = "NR_PARTIDO")
    private int NR_PARTIDO;

    @CsvBindByName(column = "SG_PARTIDO")
    private String SG_PARTIDO;

    @CsvBindByName(column = "NM_PARTIDO")
    private String NM_PARTIDO;

    @CsvBindByName(column = "DT_BU_RECEBIDO")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_BU_RECEBIDO;

    @CsvBindByName(column = "QT_APTOS")
    private int QT_APTOS;

    @CsvBindByName(column = "QT_COMPARECIMENTO")
    private int QT_COMPARECIMENTO;

    @CsvBindByName(column = "QT_ABSTENCOES")
    private int QT_ABSTENCOES;

    @CsvBindByName(column = "CD_TIPO_URNA")
    private int CD_TIPO_URNA;

    @CsvBindByName(column = "DS_TIPO_URNA")
    private String DS_TIPO_URNA;

    @CsvBindByName(column = "CD_TIPO_VOTAVEL")
    private int CD_TIPO_VOTAVEL;

    @CsvBindByName(column = "DS_TIPO_VOTAVEL")
    private String DS_TIPO_VOTAVEL;

    @CsvBindByName(column = "NR_VOTAVEL")
    private int NR_VOTAVEL;

    @CsvBindByName(column = "NM_VOTAVEL")
    private String NM_VOTAVEL;

    @CsvBindByName(column = "QT_VOTOS")
    private int QT_VOTOS;

    @CsvBindByName(column = "NR_URNA_EFETIVADA")
    private int NR_URNA_EFETIVADA;

    @CsvBindByName(column = "CD_CARGA_1_URNA_EFETIVADA")
    private String CD_CARGA_1_URNA_EFETIVADA;

    @CsvBindByName(column = "CD_CARGA_2_URNA_EFETIVADA")
    private String CD_CARGA_2_URNA_EFETIVADA;

    @CsvBindByName(column = "CD_FLASHCARD_URNA_EFETIVADA")
    private String CD_FLASHCARD_URNA_EFETIVADA;

    @CsvBindByName(column = "DT_CARGA_URNA_EFETIVADA")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_CARGA_URNA_EFETIVADA;

    @CsvBindByName(column = "DS_CARGO_PERGUNTA_SECAO")
    private String DS_CARGO_PERGUNTA_SECAO;

    @CsvBindByName(column = "DS_SECOES_AGREGADAS")
    private String DS_SECOES_AGREGADAS;

    @CsvBindByName(column = "DT_ABERTURA")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_ABERTURA;

    @CsvBindByName(column = "DT_ENCERRAMENTO")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_ENCERRAMENTO;

    @CsvBindByName(column = "QT_ELEI_BIOM_SEM_HABILITACAO")
    private int QT_ELEI_BIOM_SEM_HABILITACAO;

    @CsvBindByName(column = "DT_EMISSAO_BU")
    @CsvDate("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime DT_EMISSAO_BU;

    @CsvBindByName(column = "NR_JUNTA_APURADORA")
    private int NR_JUNTA_APURADORA;

    @CsvBindByName(column = "NR_TURMA_APURADORA")
    private int NR_TURMA_APURADORA;

    public LocalDate DT_GERACAO() { return DT_GERACAO; }
    public LocalTime HH_GERACAO() { return HH_GERACAO; }
    public int ANO_ELEICAO() { return ANO_ELEICAO; }
    public int CD_TIPO_ELEICAO() { return CD_TIPO_ELEICAO; }
    public String NM_TIPO_ELEICAO() { return NM_TIPO_ELEICAO; }
    public int CD_PLEITO() { return CD_PLEITO; }
    public LocalDateTime DT_PLEITO() { return DT_PLEITO; }
    public int NR_TURNO() { return NR_TURNO; }
    public int CD_ELEICAO() { return CD_ELEICAO; }
    public String DS_ELEICAO() { return DS_ELEICAO; }
    public String SG_UF() { return SG_UF; }
    public int CD_MUNICIPIO() { return CD_MUNICIPIO; }
    public String NM_MUNICIPIO() { return NM_MUNICIPIO; }
    public int NR_ZONA() { return NR_ZONA; }
    public int NR_SECAO() { return NR_SECAO; }
    public int NR_LOCAL_VOTACAO() { return NR_LOCAL_VOTACAO; }
    public int CD_CARGO_PERGUNTA() { return CD_CARGO_PERGUNTA; }
    public String DS_CARGO_PERGUNTA() { return DS_CARGO_PERGUNTA; }
    public int NR_PARTIDO() { return NR_PARTIDO; }
    public String SG_PARTIDO() { return SG_PARTIDO; }
    public String NM_PARTIDO() { return NM_PARTIDO; }
    public LocalDateTime DT_BU_RECEBIDO() { return DT_BU_RECEBIDO; }
    public int QT_APTOS() { return QT_APTOS; }
    public int QT_COMPARECIMENTO() { return QT_COMPARECIMENTO; }
    public int QT_ABSTENCOES() { return QT_ABSTENCOES; }
    public int CD_TIPO_URNA() { return CD_TIPO_URNA; }
    public String DS_TIPO_URNA() { return DS_TIPO_URNA; }
    public int CD_TIPO_VOTAVEL() { return CD_TIPO_VOTAVEL; }
    public String DS_TIPO_VOTAVEL() { return DS_TIPO_VOTAVEL; }
    public int NR_VOTAVEL() { return NR_VOTAVEL; }
    public String NM_VOTAVEL() { return NM_VOTAVEL; }
    public int QT_VOTOS() { return QT_VOTOS; }
    public int NR_URNA_EFETIVADA() { return NR_URNA_EFETIVADA; }
    public String CD_CARGA_1_URNA_EFETIVADA() { return CD_CARGA_1_URNA_EFETIVADA; }
    public String CD_CARGA_2_URNA_EFETIVADA() { return CD_CARGA_2_URNA_EFETIVADA; }
    public String CD_FLASHCARD_URNA_EFETIVADA() { return CD_FLASHCARD_URNA_EFETIVADA; }
    public LocalDateTime DT_CARGA_URNA_EFETIVADA() { return DT_CARGA_URNA_EFETIVADA; }
    public String DS_CARGO_PERGUNTA_SECAO() { return DS_CARGO_PERGUNTA_SECAO; }
    public String DS_SECOES_AGREGADAS() { return DS_SECOES_AGREGADAS; }
    public LocalDateTime DT_ABERTURA() { return DT_ABERTURA; }
    public LocalDateTime DT_ENCERRAMENTO() { return DT_ENCERRAMENTO; }
    public int QT_ELEI_BIOM_SEM_HABILITACAO() { return QT_ELEI_BIOM_SEM_HABILITACAO; }
    public LocalDateTime DT_EMISSAO_BU() { return DT_EMISSAO_BU; }
    public int NR_JUNTA_APURADORA() { return NR_JUNTA_APURADORA; }
    public int NR_TURMA_APURADORA() { return NR_TURMA_APURADORA; }
}
