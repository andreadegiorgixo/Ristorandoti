import { HttpErrorResponse } from '@angular/common/http';

import { ApiErrorResponse } from '../models/auth.models';

/** Errore normalizzato delle API (profilo, post), pronto per essere mostrato. */
export interface ApiError {
  /** Status HTTP; 0 se il server non è raggiungibile */
  status: number;
  message: string;
  /** Errori di validazione per campo, es. { "esperienze[0].azienda": "..." } */
  fieldErrors?: Record<string, string>;
}

/** Converte un errore HTTP nel formato del GlobalExceptionHandler in un {@link ApiError}. */
export function toApiError(err: unknown): ApiError {
  if (!(err instanceof HttpErrorResponse)) {
    return { status: -1, message: 'Si è verificato un errore imprevisto. Riprova.' };
  }

  const body = err.error as Partial<ApiErrorResponse> | null;

  if (err.status === 0) {
    return { status: 0, message: 'Impossibile contattare il server. Controlla la connessione e riprova.' };
  }

  return {
    status: err.status,
    message: body?.message ?? fallbackMessage(err.status),
    fieldErrors: body?.fieldErrors,
  };
}

function fallbackMessage(status: number): string {
  switch (status) {
    case 400:
      return 'Alcuni dati non sono validi.';
    case 401:
      return 'La sessione è scaduta. Accedi di nuovo.';
    case 404:
      return 'Contenuto non trovato.';
    case 413:
      return 'Il file è troppo grande.';
    default:
      return 'Si è verificato un errore imprevisto. Riprova.';
  }
}
