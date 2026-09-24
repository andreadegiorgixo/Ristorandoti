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

/** Fascia di prezzo (FasciaPrezzo lato backend), espressa in simboli "€". */
export type FasciaPrezzo = 'EURO_1' | 'EURO_2' | 'EURO_3' | 'EURO_4' | 'EURO_5';

/** Opzioni per la select del form, nell'ordine in cui vanno mostrate. */
export const FASCE_PREZZO: ReadonlyArray<{ value: FasciaPrezzo; simbolo: string; label: string }> = [
  { value: 'EURO_1', simbolo: '€', label: '5 - 25 €' },
  { value: 'EURO_2', simbolo: '€€', label: '25 - 50 €' },
  { value: 'EURO_3', simbolo: '€€€', label: '50 - 100 €' },
  { value: 'EURO_4', simbolo: '€€€€', label: '100 - 200 €' },
  { value: 'EURO_5', simbolo: '€€€€€', label: '200 €+' },
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
  /** URL del logo aziendale (quadrato). */
  fotoProfiloUrl: string | null;
  /** URL del banner (rettangolare). */
  bannerUrl: string | null;
  fasciaPrezzo: FasciaPrezzo | null;
  servizi: string[];
  /** Istante ISO-8601 UTC */
  dataCreazione: string;
  /** Numero di persone che seguono la pagina aziendale. */
  followersCount: number;
  /** {@code true} se l'utente autenticato segue questa pagina. */
  followedByMe: boolean;
  /** {@code true} se l'utente autenticato è proprietario o persona autorizzata: può pubblicare post e offerte. */
  gestibileDaMe: boolean;
}

/** Persona restituita da {@code GET /api/aziende/{id}/persone} (AziendaPersonaDto). */
export interface AziendaPersona {
  userId: number;
  name: string;
  profilePictureUrl: string | null;
  /** Ruolo ricoperto in azienda, es. "Sous Chef". */
  ruolo: string;
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
  fotoProfiloUrl: string;
  bannerUrl: string;
  fasciaPrezzo: FasciaPrezzo;
  servizi?: string[];
}
