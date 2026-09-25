import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { Azienda, AziendaPersona, AziendaRequest } from '../models/azienda.models';
import { Page } from '../models/post.models';

@Injectable({ providedIn: 'root' })
export class AziendaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/aziende`;

  create(payload: AziendaRequest): Observable<Azienda> {
    return this.http.post<Azienda>(this.baseUrl, payload).pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  getById(id: number): Observable<Azienda> {
    return this.http.get<Azienda>(`${this.baseUrl}/${id}`).pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Aggiorna un'azienda esistente. Il backend rifiuta con 403 chi non è il proprietario. */
  update(id: number, payload: AziendaRequest): Observable<Azienda> {
    return this.http.put<Azienda>(`${this.baseUrl}/${id}`, payload).pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Aziende di un utente, dalla più recente. */
  getByUser(userId: number, page: number, size = 20): Observable<Page<Azienda>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<Azienda>>(`${this.baseUrl}/utente/${userId}`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Ricerca per nome, in ordine alfabetico. Usata dal tipo-mentre-scrivi e dalla ricerca globale. */
  search(query: string, page = 0, size = 6): Observable<Page<Azienda>> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', size);
    return this.http
      .get<Page<Azienda>>(`${this.baseUrl}/ricerca`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Persone che lavorano attualmente nell'azienda, dalla più recente. */
  getPersone(aziendaId: number, page: number, size = 20): Observable<Page<AziendaPersona>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<AziendaPersona>>(`${this.baseUrl}/${aziendaId}/persone`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Aggiorna la descrizione della sezione Panoramica. Richiede MANAGE_OVERVIEW. */
  updatePanoramica(id: number, descrizione: string): Observable<Azienda> {
    return this.http
      .put<Azienda>(`${this.baseUrl}/${id}/panoramica`, { descrizione })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
