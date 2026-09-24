import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { Page } from '../models/post.models';
import { Review, ReviewEligibility, ReviewRequest } from '../models/review.models';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reviews`;

  /** Recensioni ricevute da un utente (colleghi/ex colleghi), dalla più recente. */
  getForUser(userId: number, page: number, size = 10): Observable<Page<Review>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<Page<Review>>(`${this.baseUrl}/user/${userId}`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Dice se l'utente autenticato può recensire userId (esperienza sovrapposta) e se lo ha già fatto. */
  getEligibility(userId: number): Observable<ReviewEligibility> {
    return this.http
      .get<ReviewEligibility>(`${this.baseUrl}/eligibility/${userId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Crea o aggiorna la recensione dell'utente autenticato verso userId. */
  upsert(userId: number, request: ReviewRequest): Observable<Review> {
    return this.http
      .put<Review>(`${this.baseUrl}/${userId}`, request)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Elimina la recensione dell'utente autenticato verso userId. */
  delete(userId: number): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${userId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
