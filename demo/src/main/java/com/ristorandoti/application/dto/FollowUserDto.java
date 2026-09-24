package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Utente restituito nelle liste {@code GET /api/follows/{userId}/followers} e
 * {@code GET /api/follows/{userId}/following}. Contiene solo i dati utili a una card
 * "persona" (nome, foto, headline), non il profilo completo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUserDto {

    private Long userId;

    private String name;

    private String profilePictureUrl;

    /** Headline professionale. */
    private String sommario;

    /** {@code true} se l'utente che fa la richiesta segue già questa persona. */
    private boolean followedByMe;
}
