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

  /** Chiude (elimina) un'offerta, liberando uno slot per una nuova. */
  chiudi(aziendaId: number, offertaId: number): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/aziende/${aziendaId}/offerte-lavoro/${offertaId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
