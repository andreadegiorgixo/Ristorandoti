package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persona in uscita da {@code GET /api/profiles/ricerca}: versione leggera del profilo,
 * senza esperienze/istruzione/lingue, pensata per la ricerca globale.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonaSearchResultDto {

    private Long userId;

    private String name;

    private String profilePictureUrl;

    /** Headline professionale, es. "Executive Chef presso Ristorante Da Mario". */
    private String sommario;
}
