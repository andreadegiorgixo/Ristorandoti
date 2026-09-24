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
