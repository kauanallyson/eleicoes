/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */

package br.com.kauanallyson.eleicoes.apuracao;

import br.com.kauanallyson.eleicoes.bweb.Voto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * Os horários do boletim de urna estão no fuso local da seção.
 * Desde 2022 a votação no Brasil segue o horário de Brasília (8h às 17h),
 * então para comparar com o fim da votação é preciso converter.
 */
public class FusoHorario {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final ZoneId NORONHA = ZoneId.of("America/Noronha");
    private static final ZoneId OESTE_DO_AMAZONAS = ZoneId.of("America/Eirunepe");

    private static final Map<String, ZoneId> FUSO_POR_UF = Map.of(
            "AC", ZoneId.of("America/Rio_Branco"),
            "AM", ZoneId.of("America/Manaus"),
            "MS", ZoneId.of("America/Campo_Grande"),
            "MT", ZoneId.of("America/Cuiaba"),
            "RO", ZoneId.of("America/Porto_Velho"),
            "RR", ZoneId.of("America/Boa_Vista"));

    /** Municípios do oeste do AM que ficam em UTC-5 (código TSE). */
    private static final Set<Integer> AM_UTC_5 = Set.of(
            2020,  // GUAJARÁ
            2054,  // ATALAIA DO NORTE
            2135,  // BENJAMIN CONSTANT
            2291,  // EIRUNEPÉ
            2313,  // ENVIRA
            2399,  // IPIXUNA
            2496,  // JUTAÍ
            2755,  // SÃO PAULO DE OLIVENÇA
            2917,  // AMATURÁ
            98370, // ITAMARATI
            98477  // TABATINGA
    );

    private static final int FERNANDO_DE_NORONHA = 30015;

    /**
     * Converte um horário local do boletim para o horário de Brasília.
     * Seções do exterior (ZZ) votam no horário local do país, então são mantidas como estão.
     */
    public LocalDateTime emBrasilia(Voto voto, LocalDateTime horarioLocal) {
        return emBrasilia(voto.SG_UF(), voto.CD_MUNICIPIO(), horarioLocal);
    }

    /** Mesmo que {@link #emBrasilia(Voto, LocalDateTime)}, a partir da UF e do código TSE do município. */
    public LocalDateTime emBrasilia(String uf, int municipio, LocalDateTime horarioLocal) {
        if (uf.equals("ZZ")) return horarioLocal;

        return horarioLocal.atZone(fusoDaSecao(uf, municipio))
                .withZoneSameInstant(BRASILIA)
                .toLocalDateTime();
    }

    private ZoneId fusoDaSecao(String uf, int municipio) {
        if (municipio == FERNANDO_DE_NORONHA) return NORONHA;
        if (AM_UTC_5.contains(municipio)) return OESTE_DO_AMAZONAS;
        return FUSO_POR_UF.getOrDefault(uf, BRASILIA);
    }
}
