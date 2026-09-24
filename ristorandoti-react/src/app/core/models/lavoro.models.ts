/** Offerta di lavoro restituita dalle API di {@code /api/aziende/{aziendaId}/offerte-lavoro} (OffertaLavoroDto). */
export interface OffertaLavoro {
  id: number;
  aziendaId: number;
  aziendaNome: string;
  autoreId: number;
  autoreName: string;
  titolo: string;
  descrizione: string | null;
  /** Istante ISO-8601 UTC */
  dataCreazione: string;
}

/** Body di {@code POST /api/aziende/{aziendaId}/offerte-lavoro} (OffertaLavoroRequestDto). */
export interface OffertaLavoroRequest {
  titolo: string;
  descrizione?: string;
}

/** Numero massimo di offerte di lavoro attive per azienda (allineato a OffertaLavoroService). */
export const MAX_OFFERTE_ATTIVE = 3;
