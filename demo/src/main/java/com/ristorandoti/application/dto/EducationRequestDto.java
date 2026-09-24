package com.ristorandoti.application.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Percorso di studio in ingresso, parte di {@link ProfileUpdateRequestDto}.
 * Non ha {@code id}: la lista inviata sostituisce interamente quella salvata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EducationRequestDto {

    @NotBlank(message = "L'istituto è obbligatorio")
    @Size(max = 200, message = "L'istituto non può superare 200 caratteri")
    private String istituto;

    @NotBlank(message = "Il titolo di studio è obbligatorio")
    @Size(max = 200, message = "Il titolo di studio non può superare 200 caratteri")
    private String titoloStudio;

    @NotNull(message = "La data di inizio è obbligatoria")
    @PastOrPresent(message = "La data di inizio non può essere nel futuro")
    private LocalDate dataStart;

    /** Vuota se il percorso è ancora in corso. */
    private LocalDate dataEnd;

    /** Regola cross-field: la fine, se presente, non può precedere l'inizio. */
    @JsonIgnore
    @AssertTrue(message = "La data di fine non può precedere la data di inizio")
    public boolean isPeriodoValido() {
        return dataStart == null || dataEnd == null || !dataEnd.isBefore(dataStart);
    }
}
