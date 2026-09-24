/** Esperienza lavorativa (ExperienceDto). Le date sono ISO "AAAA-MM-GG". */
export interface Experience {
  id: number;
  azienda: string;
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
  followersCount: number;
  followingCount: number;
  /** true se l'utente loggato segue questo profilo (sempre false sul proprio). */
  followedByMe: boolean;
  recensioniCount: number;
  /** Media dei voti ricevuti (1-5), null se non ha ancora recensioni. */
  valutazioneMedia: number | null;
}

export type ExperienceRequest = Omit<Experience, 'id'>;
export type EducationRequest = Omit<Education, 'id'>;

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
}
