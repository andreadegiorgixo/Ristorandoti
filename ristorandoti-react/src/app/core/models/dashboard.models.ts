/** Capability della Dashboard aziendale (Capability lato backend). */
export type Capability = 'VIEW_DASHBOARD' | 'MANAGE_POSTS' | 'MANAGE_JOBS' | 'MANAGE_OVERVIEW' | 'MANAGE_PERMISSIONS';

/** Ruolo assegnabile a un dipendente (AziendaRuoloCodice lato backend). */
export type AziendaRuoloCodice = 'ADMIN' | 'HR' | 'SOCIAL_MEDIA_MANAGER';

/** Etichette in italiano dei ruoli, nell'ordine in cui vanno mostrate. */
export const RUOLI_ASSEGNABILI: ReadonlyArray<{ value: AziendaRuoloCodice; label: string }> = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'HR', label: 'Risorse Umane' },
  { value: 'SOCIAL_MEDIA_MANAGER', label: 'Social Media Manager' },
];

/** Etichette delle capability, per la legenda della Dashboard. */
export const CAPABILITY_LABEL: Record<Capability, string> = {
  VIEW_DASHBOARD: 'Accesso alla Dashboard',
  MANAGE_POSTS: 'Gestione post',
  MANAGE_JOBS: 'Gestione offerte di lavoro',
  MANAGE_OVERVIEW: 'Gestione panoramica',
  MANAGE_PERMISSIONS: 'Gestione permessi',
};

/** Risposta di {@code GET .../dashboard/permessi/correnti} (AziendaPermessiCorrentiDto). */
export interface AziendaPermessiCorrenti {
  proprietario: boolean;
  capabilities: Capability[];
  ruoli: AziendaRuoloCodice[];
}

/** Ruolo del catalogo e capability che comporta (AziendaRuoloDto). */
export interface AziendaRuoloCatalogo {
  codice: AziendaRuoloCodice;
  nome: string;
  capabilities: Capability[];
}

/** Dipendente attualmente assunto, con i ruoli attivi (AziendaDipendenteRuoliDto). */
export interface AziendaDipendenteRuoli {
  userId: number;
  name: string;
  profilePictureUrl: string | null;
  ruoli: AziendaRuoloCodice[];
}

/** Voce del log di audit dei permessi (AziendaAuditLogDto). */
export interface AziendaAuditLogEntry {
  id: number;
  attoreId: number;
  attoreName: string;
  targetUserId: number;
  targetUserName: string;
  ruoloCodice: AziendaRuoloCodice;
  azione: 'ASSEGNATO' | 'REVOCATO' | 'REVOCATO_FINE_RAPPORTO';
  dettaglio: string | null;
  dataEvento: string;
}

/** Metrica della Home Dashboard (MetricaDashboard lato backend). */
export type MetricaDashboard = 'VISUALIZZATORI_UNICI' | 'FOLLOWER' | 'LIKE' | 'RICERCHE';

/** Etichetta e colore di ciascuna metrica, nell'ordine in cui vanno mostrate le card. */
export const METRICHE_DASHBOARD: ReadonlyArray<{ value: MetricaDashboard; label: string; colore: string }> = [
  { value: 'VISUALIZZATORI_UNICI', label: 'Visualizzatori', colore: '#3e6a51' },
  { value: 'FOLLOWER', label: 'Follower', colore: '#b45309' },
  { value: 'LIKE', label: 'Like ai post', colore: '#be123c' },
  { value: 'RICERCHE', label: 'Ricerche', colore: '#1d4ed8' },
];

/** Un punto della serie temporale di una metrica (MetricSeriePuntoDto). */
export interface MetricSeriePunto {
  /** Data ISO (yyyy-MM-dd) */
  giorno: string;
  valore: number;
}

/** Serie temporale e statistiche di periodo di una metrica (MetricSerieDto). */
export interface MetricSerie {
  metrica: MetricaDashboard;
  punti: MetricSeriePunto[];
  totalePeriodo: number;
  variazionePercento: number | null;
  /** Data ISO del primo giorno con dati disponibili per l'azienda; {@code null} se nessuno ancora. */
  datiDisponibiliDal: string | null;
}

/** Risposta di {@code GET .../dashboard/metriche} (DashboardMetricheDto). */
export interface DashboardMetriche {
  serie: MetricSerie[];
}
