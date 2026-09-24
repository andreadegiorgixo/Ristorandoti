package com.ristorandoti.application.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recensione restituita da {@code GET /api/reviews/user/{userId}} e
 * {@code PUT /api/reviews/{userId}}. Include i dati dell'autore utili alla card
 * (nome, foto, headline), così il client non deve caricare il suo profilo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

    private Long id;

    private Long autoreId;

    private String autoreName;

    private String autoreProfilePictureUrl;

    /** Headline dell'autore, es. "Executive Chef presso Ristorante Da Mario". */
    private String autoreSommario;

    private Long destinatarioId;

    /** Voto da 1 a 5. */
    private int valutazione;

    private String testo;

    private Instant dataCreazione;

    private Instant dataAggiornamento;
}
