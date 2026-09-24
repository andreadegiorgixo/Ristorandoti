package com.ristorandoti.application.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Percorso di studio in uscita, parte di {@link ProfileDto}.
 * Le date sono serializzate in formato ISO ({@code "2024-03-01"}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationDto {

    private Long id;

    private String istituto;

    private String titoloStudio;

    private LocalDate dataStart;

    /** {@code null} se il percorso è ancora in corso. */
    private LocalDate dataEnd;
}
