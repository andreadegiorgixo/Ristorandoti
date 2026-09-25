import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { Candidatura, CandidaturaRequest } from '../models/lavoro.models';
import { Page } from '../models/post.models';

/** Candidature a un'offerta di lavoro: candidarsi (chiunque) ed elenco candidati (solo MANAGE_JOBS). */
@Injectable({ providedIn: 'root' })
export class CandidaturaService {
  private readonly http = inject(HttpClient);

  candidati(aziendaId: number, offertaId: number, payload: CandidaturaRequest = {}): Observable<Candidatura> {
    return this.http
      .post<Candidatura>(`${environment.apiUrl}/aziende/${aziendaId}/offerte-lavoro/${offertaId}/candidature`, payload)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  getCandidati(aziendaId: number, offertaId: number, page = 0, size = 20): Observable<Page<Candidatura>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<Candidatura>>(`${environment.apiUrl}/aziende/${aziendaId}/offerte-lavoro/${offertaId}/candidature`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
