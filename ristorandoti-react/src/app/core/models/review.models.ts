/** Recensione ricevuta da un utente, lasciata da un collega/ex collega (ReviewDto). */
export interface Review {
  id: number;
  autoreId: number;
  autoreName: string;
  autoreProfilePictureUrl: string | null;
  autoreSommario: string | null;
  destinatarioId: number;
  /** Voto da 1 a 5. */
  valutazione: number;
  testo: string;
  /** Istanti ISO-8601 UTC */
  dataCreazione: string;
  dataAggiornamento: string;
}

/** Dati per creare o aggiornare una recensione (ReviewRequestDto). */
export interface ReviewRequest {
  /** Voto da 1 a 5. */
  valutazione: number;
  testo: string;
}

/** Risposta di GET /api/reviews/eligibility/{userId} (ReviewEligibilityDto). */
export interface ReviewEligibility {
  /** true se l'utente autenticato può recensire questo utente (esperienza sovrapposta, non se stesso). */
  canReview: boolean;
  /** true se l'utente autenticato ha già recensito questo utente. */
  alreadyReviewed: boolean;
  /** Recensione già lasciata dall'utente autenticato, se alreadyReviewed è true. */
  myReview: Review | null;
}
