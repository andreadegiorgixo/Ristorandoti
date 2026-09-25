import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import {
  AziendaAuditLogEntry,
  AziendaDipendenteRuoli,
  AziendaPermessiCorrenti,
  AziendaRuoloCatalogo,
  AziendaRuoloCodice,
  Capability,
} from '../models/dashboard.models';
import { Page } from '../models/post.models';

/**
 * Capability e ruoli dell'utente autenticato sulla pagina aziendale correntemente aperta nella
 * Dashboard. {@link refresh} va chiamato dalla guardia di accesso e di nuovo ogni volta che un
 * endpoint della Dashboard risponde 403 (permesso revocato a metà sessione), così le sezioni
 * tornano in sola lettura senza un errore confuso.
 */
@Injectable({ providedIn: 'root' })
export class DashboardPermissionService {
  private readonly http = inject(HttpClient);

  private readonly _aziendaId = signal<number | null>(null);
  private readonly _permessi = signal<AziendaPermessiCorrenti | null>(null);

  readonly aziendaId = this._aziendaId.asReadonly();
  readonly permessi = this._permessi.asReadonly();

  private readonly _capabilities = computed(() => new Set(this._permessi()?.capabilities ?? []));

  /** {@code true} se le capability correnti includono quella richiesta (il proprietario le ha sempre tutte). */
  can(capability: Capability): boolean {
    return this._capabilities().has(capability);
  }

  /** Ricarica le capability correnti per l'azienda indicata. */
  refresh(aziendaId: number): Observable<AziendaPermessiCorrenti> {
    return this.http.get<AziendaPermessiCorrenti>(`${this.baseUrl(aziendaId)}/correnti`).pipe(
      tap((permessi) => {
        this._aziendaId.set(aziendaId);
        this._permessi.set(permessi);
      }),
      catchError((err) => throwError(() => toApiError(err))),
    );
  }

  catalogoRuoli(aziendaId: number): Observable<AziendaRuoloCatalogo[]> {
    return this.http
      .get<AziendaRuoloCatalogo[]>(`${this.baseUrl(aziendaId)}/ruoli`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  dipendenti(aziendaId: number): Observable<AziendaDipendenteRuoli[]> {
    return this.http
      .get<AziendaDipendenteRuoli[]>(this.baseUrl(aziendaId))
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  assegna(aziendaId: number, userId: number, ruolo: AziendaRuoloCodice): Observable<void> {
    return this.http
      .post<void>(`${this.baseUrl(aziendaId)}/${userId}`, { ruolo })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  revoca(aziendaId: number, userId: number, ruolo: AziendaRuoloCodice): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl(aziendaId)}/${userId}/${ruolo}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  audit(aziendaId: number, page: number, size = 20): Observable<Page<AziendaAuditLogEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<AziendaAuditLogEntry>>(`${this.baseUrl(aziendaId)}/audit`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  private baseUrl(aziendaId: number): string {
    return `${environment.apiUrl}/aziende/${aziendaId}/dashboard/permessi`;
  }
}
