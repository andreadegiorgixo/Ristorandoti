import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { CreatePostRequest, Page, Post } from '../models/post.models';

@Injectable({ providedIn: 'root' })
export class PostService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/posts`;

  /** Feed della community, dal più recente. */
  getFeed(page: number, size = 10): Observable<Page<Post>> {
    return this.http
      .get<Page<Post>>(this.baseUrl, { params: pageParams(page, size) })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  getByUser(userId: number, page: number, size = 10): Observable<Page<Post>> {
    return this.http
      .get<Page<Post>>(`${this.baseUrl}/user/${userId}`, { params: pageParams(page, size) })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  create(payload: CreatePostRequest): Observable<Post> {
    return this.http.post<Post>(this.baseUrl, payload).pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  like(postId: number): Observable<Post> {
    return this.http
      .post<Post>(`${this.baseUrl}/${postId}/like`, null)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  unlike(postId: number): Observable<Post> {
    return this.http
      .delete<Post>(`${this.baseUrl}/${postId}/like`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}

function pageParams(page: number, size: number): HttpParams {
  return new HttpParams().set('page', page).set('size', size);
}
