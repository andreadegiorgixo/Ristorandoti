package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persona restituita da {@code GET /api/aziende/{id}/persone}: chi lavora attualmente
 * nell'azienda, ricavato dalle esperienze lavorative correnti collegate ({@code Experience}
 * con {@code aziendaCollegata} = questa azienda e nessuna data di fine).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaPersonaDto {

    private Long userId;

    private String name;

    private String profilePictureUrl;

    /** Ruolo ricoperto in azienda, es. "Sous Chef". */
    private String ruolo;
}
