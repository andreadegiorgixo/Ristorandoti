import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { CreatePostRequest, Page, Post, PostVisibilita } from '../models/post.models';

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

  /** Post pubblicati come pagina aziendale, dal più recente. */
  getByAzienda(aziendaId: number, page: number, size = 10): Observable<Page<Post>> {
    return this.http
      .get<Page<Post>>(`${this.baseUrl}/azienda/${aziendaId}`, { params: pageParams(page, size) })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  create(payload: CreatePostRequest): Observable<Post> {
    return this.http.post<Post>(this.baseUrl, payload).pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Pubblica un post come pagina aziendale. Solo proprietario o persone autorizzate. */
  createForAzienda(aziendaId: number, payload: CreatePostRequest): Observable<Post> {
    return this.http
      .post<Post>(`${this.baseUrl}/azienda/${aziendaId}`, payload)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
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

  /** Post di un'azienda per la Dashboard: sia pubblici sia privati, filtro di stato opzionale. */
  getByAziendaDashboard(aziendaId: number, page: number, size = 20, stato?: PostVisibilita): Observable<Page<Post>> {
    let params = pageParams(page, size);
    if (stato) params = params.set('stato', stato);
    return this.http
      .get<Page<Post>>(`${environment.apiUrl}/aziende/${aziendaId}/posts`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Modifica testo/foto di un post di pagina aziendale già pubblicato. Richiede MANAGE_POSTS. */
  updateAzienda(aziendaId: number, postId: number, payload: CreatePostRequest): Observable<Post> {
    return this.http
      .put<Post>(`${environment.apiUrl}/aziende/${aziendaId}/posts/${postId}`, payload)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Nasconde un post (visibilità PRIVATO, non lo cancella). Richiede MANAGE_POSTS. */
  nascondi(aziendaId: number, postId: number): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/aziende/${aziendaId}/posts/${postId}/nascondi`, {})
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Rende di nuovo pubblico un post nascosto. Richiede MANAGE_POSTS. */
  mostra(aziendaId: number, postId: number): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/aziende/${aziendaId}/posts/${postId}/mostra`, {})
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Rimuove un post (soft-delete: sparisce ovunque, i like restano nel totale storico). Richiede MANAGE_POSTS. */
  rimuovi(aziendaId: number, postId: number): Observable<void> {
    return this.http
      .delete<void>(`${environment.apiUrl}/aziende/${aziendaId}/posts/${postId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}

function pageParams(page: number, size: number): HttpParams {
  return new HttpParams().set('page', page).set('size', size);
}
