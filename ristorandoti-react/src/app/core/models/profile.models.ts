/** Esperienza lavorativa (ExperienceDto). Le date sono ISO "AAAA-MM-GG". */
export interface Experience {
  id: number;
  azienda: string;
  /** Id dell'azienda registrata collegata; null se nessuna corrispondenza. */
  aziendaId: number | null;
  /** URL del logo dell'azienda registrata collegata; null se nessuna corrispondenza. */
  aziendaLogoUrl: string | null;
  ruolo: string;
  dataStart: string;
  /** null = posizione attuale */
  dataEnd: string | null;
  descrizione: string | null;
}

/** Percorso di studio (EducationDto). */
export interface Education {
  id: number;
  istituto: string;
  titoloStudio: string;
  dataStart: string;
  /** null = in corso */
  dataEnd: string | null;
}

/** Livello di conoscenza di una lingua (LanguageLevel), da base a madrelingua. */
export type LanguageLevel = 'BASE' | 'INTERMEDIO' | 'CONOSCENZA_PROFESSIONALE' | 'MADRELINGUA';

/** Lingua conosciuta (LanguageDto), con livello separato per scritto e parlato. */
export interface Language {
  id: number;
  lingua: string;
  livelloScritto: LanguageLevel;
  livelloParlato: LanguageLevel;
}

/** Risposta di GET /api/profiles/me e /api/profiles/{userId} (ProfileDto) */
export interface Profile {
  id: number;
  userId: number;
  name: string;
  profilePictureUrl: string | null;
  bannerUrl: string | null;
  sommario: string | null;
  esperienze: Experience[];
  istruzione: Education[];
  /** Lingue conosciute, in ordine alfabetico. */
  lingue: Language[];
  followersCount: number;
  followingCount: number;
  /** true se l'utente loggato segue questo profilo (sempre false sul proprio). */
  followedByMe: boolean;
  recensioniCount: number;
  /** Media dei voti ricevuti (1-5), null se non ha ancora recensioni. */
  valutazioneMedia: number | null;
}

export type ExperienceRequest = Omit<Experience, 'id' | 'aziendaLogoUrl'>;
export type EducationRequest = Omit<Education, 'id'>;
export type LanguageRequest = Omit<Language, 'id'>;

/**
 * Body di PUT /api/profiles/me (ProfileUpdateRequestDto).
 * Campo assente = invariato, "" = svuota, liste = sostituzione completa.
 */
export interface ProfileUpdateRequest {
  profilePictureUrl?: string;
  bannerUrl?: string;
  sommario?: string;
  esperienze?: ExperienceRequest[];
  istruzione?: EducationRequest[];
  lingue?: LanguageRequest[];
}
