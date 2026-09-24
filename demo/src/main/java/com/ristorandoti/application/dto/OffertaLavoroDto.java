package com.ristorandoti.application.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Offerta di lavoro restituita dalle API di {@code /api/aziende/{aziendaId}/offerte-lavoro}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffertaLavoroDto {

    private Long id;

    private Long aziendaId;

    private String aziendaNome;

    private Long autoreId;

    private String autoreName;

    private String titolo;

    private String descrizione;

    /** Istante di pubblicazione in formato ISO-8601 UTC. */
    private Instant dataCreazione;
}
