import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { OffertaLavoro, OffertaLavoroRequest } from '../models/lavoro.models';
import { Page } from '../models/post.models';

@Injectable({ providedIn: 'root' })
export class OffertaLavoroService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /** Offerte di lavoro attive di un'azienda, dalla più recente. */
  getByAzienda(aziendaId: number, page = 0, size = 20): Observable<Page<OffertaLavoro>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<OffertaLavoro>>(`${this.baseUrl}/aziende/${aziendaId}/offerte-lavoro`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Pubblica una nuova offerta. Il backend rifiuta con 400 oltre le 3 offerte attive. */
  create(aziendaId: number, payload: OffertaLavoroRequest): Observable<OffertaLavoro> {
    return this.http
      .post<OffertaLavoro>(`${this.baseUrl}/aziende/${aziendaId}/offerte-lavoro`, payload)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Tutte le offerte dell'azienda (attive e scadute), per la Dashboard. */
  getByAziendaDashboard(aziendaId: number, page = 0, size = 20): Observable<Page<OffertaLavoro>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<OffertaLavoro>>(`${this.baseUrl}/aziende/${aziendaId}/offerte-lavoro/dashboard`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Modifica titolo/descrizione. Il backend rifiuta con 400 se l'offerta è scaduta nel frattempo. */
  update(aziendaId: number, offertaId: number, payload: OffertaLavoroRequest): Observable<OffertaLavoro> {
    return this.http
      .put<OffertaLavoro>(`${this.baseUrl}/aziende/${aziendaId}/offerte-lavoro/${offertaId}`, payload)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Chiude (elimina) un'offerta, liberando uno slot per una nuova. */
  chiudi(aziendaId: number, offertaId: number): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/aziende/${aziendaId}/offerte-lavoro/${offertaId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Ricerca globale (tutte le aziende) delle offerte attive per titolo. Usata dalla ricerca globale in navbar e dalla pagina risultati. */
  search(query: string, page = 0, size = 6): Observable<Page<OffertaLavoro>> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', size);
    return this.http
      .get<Page<OffertaLavoro>>(`${this.baseUrl}/offerte-lavoro/ricerca`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
