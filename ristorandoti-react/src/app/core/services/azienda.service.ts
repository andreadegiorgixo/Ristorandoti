import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { Azienda, AziendaRequest } from '../models/azienda.models';
import { Page } from '../models/post.models';

@Injectable({ providedIn: 'root' })
export class AziendaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/aziende`;

  create(payload: AziendaRequest): Observable<Azienda> {
    return this.http.post<Azienda>(this.baseUrl, payload).pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Aziende di un utente, dalla più recente. */
  getByUser(userId: number, page: number, size = 20): Observable<Page<Azienda>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<Azienda>>(`${this.baseUrl}/utente/${userId}`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Ricerca per nome, in ordine alfabetico. Usata dal tipo-mentre-scrivi. */
  search(query: string, size = 6): Observable<Page<Azienda>> {
    const params = new HttpParams().set('q', query).set('page', 0).set('size', size);
    return this.http
      .get<Page<Azienda>>(`${this.baseUrl}/ricerca`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
