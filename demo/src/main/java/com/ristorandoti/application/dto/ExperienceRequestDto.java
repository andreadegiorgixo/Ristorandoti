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
 * Esperienza lavorativa in ingresso, parte di {@link ProfileUpdateRequestDto}.
 * Non ha {@code id}: la lista inviata sostituisce interamente quella salvata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ExperienceRequestDto {

    @NotBlank(message = "L'azienda è obbligatoria")
    @Size(max = 200, message = "L'azienda non può superare 200 caratteri")
    private String azienda;

    /**
     * Id dell'azienda registrata scelta dalla ricerca, se corrisponde. {@code null} se l'utente
     * ha lasciato solo il testo libero, oppure se l'azienda scelta non esiste più: in entrambi i
     * casi l'esperienza viene comunque salvata con il solo nome.
     */
    private Long aziendaId;

    @NotBlank(message = "Il ruolo è obbligatorio")
    @Size(max = 150, message = "Il ruolo non può superare 150 caratteri")
    private String ruolo;

    @NotNull(message = "La data di inizio è obbligatoria")
    @PastOrPresent(message = "La data di inizio non può essere nel futuro")
    private LocalDate dataStart;

    /** Vuota per la posizione attuale. */
    private LocalDate dataEnd;

    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;

    /** Regola cross-field: la fine, se presente, non può precedere l'inizio. */
    @JsonIgnore
    @AssertTrue(message = "La data di fine non può precedere la data di inizio")
    public boolean isPeriodoValido() {
        return dataStart == null || dataEnd == null || !dataEnd.isBefore(dataStart);
    }
}
