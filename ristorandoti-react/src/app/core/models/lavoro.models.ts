/** Stato effettivo di un'offerta di lavoro, ricalcolato dal backend a ogni lettura. */
export type StatoOffertaLavoro = 'ATTIVA' | 'SCADUTA';

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
  /** Istante ISO-8601 UTC: dataCreazione + 7 giorni */
  dataScadenza: string;
  stato: StatoOffertaLavoro;
  /** {@code true} se l'utente autenticato si è già candidato per questa offerta. */
  candidaturaGiaInviata: boolean;
}

/** Body di {@code POST/PUT .../offerte-lavoro} (OffertaLavoroRequestDto). */
export interface OffertaLavoroRequest {
  titolo: string;
  descrizione?: string;
}

/** Numero massimo di offerte di lavoro attive per azienda (allineato a DashboardProperties.maxOfferteAttive). */
export const MAX_OFFERTE_ATTIVE = 3;

/** Stato di una candidatura (versione minima del flusso). */
export type StatoCandidatura = 'INVIATA' | 'VISUALIZZATA' | 'ACCETTATA' | 'RIFIUTATA';

/** Candidatura a un'offerta di lavoro (CandidaturaDto). */
export interface Candidatura {
  id: number;
  offertaId: number;
  candidatoId: number;
  candidatoName: string;
  candidatoProfilePictureUrl: string | null;
  messaggio: string | null;
  stato: StatoCandidatura;
  /** Istante ISO-8601 UTC */
  dataCreazione: string;
}

/** Body di {@code POST .../candidature} (CandidaturaRequestDto). */
export interface CandidaturaRequest {
  messaggio?: string;
}
