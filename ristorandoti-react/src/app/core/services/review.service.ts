import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { Page } from '../models/post.models';
import { Review } from '../models/review.models';

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
}
