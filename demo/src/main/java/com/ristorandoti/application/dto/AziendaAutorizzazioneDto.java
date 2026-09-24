package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persona autorizzata a gestire una pagina aziendale, restituita solo al proprietario da
 * {@code GET /api/aziende/{id}/autorizzazioni}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaAutorizzazioneDto {

    private Long userId;

    private String name;

    private String profilePictureUrl;
}
