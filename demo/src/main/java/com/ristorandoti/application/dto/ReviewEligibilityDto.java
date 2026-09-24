package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Risposta di {@code GET /api/reviews/eligibility/{userId}}: dice al client se e come mostrare
 * il pulsante "Lascia una recensione" per l'utente autenticato verso {@code userId}.
 *
 * <p>È solo un aiuto per la UI: il controllo che conta è comunque rifatto lato server dentro
 * {@code PUT /api/reviews/{userId}}, che rifiuta la richiesta anche se il client ignora questo DTO.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEligibilityDto {

    /**
     * {@code true} se l'utente autenticato e {@code userId} hanno lavorato nello stesso locale
     * in un periodo sovrapposto (requisito per poter recensire), ed {@code userId} non è
     * l'utente stesso.
     */
    private boolean canReview;

    /** {@code true} se l'utente autenticato ha già recensito {@code userId}. */
    private boolean alreadyReviewed;

    /** Recensione già lasciata dall'utente autenticato a {@code userId}; {@code null} se {@code alreadyReviewed} è false. */
    private ReviewDto myReview;
}
