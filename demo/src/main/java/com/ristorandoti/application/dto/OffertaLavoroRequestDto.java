package com.ristorandoti.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code POST /api/aziende/{aziendaId}/offerte-lavoro}. L'azienda e l'autore
 * non si indicano qui: arrivano rispettivamente dal path e dal JWT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OffertaLavoroRequestDto {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 200, message = "Il titolo non può superare 200 caratteri")
    private String titolo;

    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;
}
