package com.ristorandoti.application.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Esperienza lavorativa in uscita, parte di {@link ProfileDto}.
 * Le date sono serializzate in formato ISO ({@code "2024-03-01"}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceDto {

    private Long id;

    private String azienda;

    /** Id dell'azienda registrata collegata; {@code null} se nessuna corrispondenza. */
    private Long aziendaId;

    private String ruolo;

    private LocalDate dataStart;

    /** {@code null} se è la posizione attuale. */
    private LocalDate dataEnd;

    private String descrizione;
}
