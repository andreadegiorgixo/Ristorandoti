import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { FollowStatus } from '../models/follow.models';

@Injectable({ providedIn: 'root' })
export class FollowService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/follows`;

  follow(userId: number): Observable<FollowStatus> {
    return this.http
      .post<FollowStatus>(`${this.baseUrl}/${userId}`, null)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  unfollow(userId: number): Observable<FollowStatus> {
    return this.http
      .delete<FollowStatus>(`${this.baseUrl}/${userId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}
