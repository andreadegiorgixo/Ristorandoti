/** Tipologia di locale (TipoAzienda lato backend). */
export type TipoAzienda = 'RISTORANTE' | 'PIZZERIA' | 'BAR' | 'HOTEL' | 'CATERING' | 'ALTRO';

/** Opzioni per la select del form, nell'ordine in cui vanno mostrate. */
export const TIPI_AZIENDA: ReadonlyArray<{ value: TipoAzienda; label: string }> = [
  { value: 'RISTORANTE', label: 'Ristorante' },
  { value: 'PIZZERIA', label: 'Pizzeria' },
  { value: 'BAR', label: 'Bar' },
  { value: 'HOTEL', label: 'Hotel' },
  { value: 'CATERING', label: 'Catering' },
  { value: 'ALTRO', label: 'Altro' },
];

/** Risposta delle API di {@code /api/aziende} (AziendaDto). */
export interface Azienda {
  id: number;
  proprietarioId: number;
  proprietarioName: string;
  nome: string;
  tipo: TipoAzienda;
  descrizione: string | null;
  indirizzo: string | null;
  citta: string | null;
  telefono: string | null;
  email: string | null;
  sitoWebUrl: string | null;
  /** Istante ISO-8601 UTC */
  dataCreazione: string;
}

/** Body di {@code POST/PUT /api/aziende} (AziendaRequestDto). */
export interface AziendaRequest {
  nome: string;
  tipo: TipoAzienda;
  descrizione?: string;
  indirizzo?: string;
  citta?: string;
  telefono?: string;
  email?: string;
  sitoWebUrl?: string;
}
