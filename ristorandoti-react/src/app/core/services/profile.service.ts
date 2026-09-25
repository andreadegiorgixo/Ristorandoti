import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { Page } from '../models/post.models';
import { PersonaSearchResult, Profile, ProfileUpdateRequest } from '../models/profile.models';
import { AuthService } from './auth.service';

/**
 * API del profilo + cache del profilo dell'utente loggato ({@link me}), condivisa da navbar,
 * feed e composer: così la foto aggiornata compare ovunque senza ricaricare.
 */
@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/profiles`;

  private readonly _me = signal<Profile | null>(null);
  readonly me = this._me.asReadonly();

  /** Voci che rendono un profilo completo, con la percentuale raggiunta. */
  readonly completion = computed(() => {
    const p = this._me();
    const steps = [
      { label: 'Foto profilo', done: !!p?.profilePictureUrl },
      { label: 'Sommario', done: !!p?.sommario },
      { label: 'Un’esperienza', done: !!p?.esperienze.length },
      { label: 'La tua formazione', done: !!p?.istruzione.length },
      { label: 'Banner', done: !!p?.bannerUrl },
    ];
    const done = steps.filter((s) => s.done).length;
    return { steps, percent: Math.round((done / steps.length) * 100) };
  });

  constructor() {
    // Carica il profilo al login, lo svuota al logout
    effect(() => {
      const user = this.auth.currentUser();
      untracked(() => {
        if (!user) {
          this._me.set(null);
        } else if (this._me()?.userId !== user.id) {
          this.getMe().subscribe({ error: () => undefined });
        }
      });
    });
  }

  getMe(): Observable<Profile> {
    return this.http.get<Profile>(`${this.baseUrl}/me`).pipe(
      tap((profile) => this._me.set(profile)),
      catchError((err) => throwError(() => toApiError(err))),
    );
  }

  getByUserId(userId: number): Observable<Profile> {
    return this.http
      .get<Profile>(`${this.baseUrl}/${userId}`)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /** Ricerca persone per nome o headline. Usata dalla ricerca globale in navbar e dalla pagina risultati. */
  search(query: string, page = 0, size = 6): Observable<Page<PersonaSearchResult>> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', size);
    return this.http
      .get<Page<PersonaSearchResult>>(`${this.baseUrl}/ricerca`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  updateMe(payload: ProfileUpdateRequest): Observable<Profile> {
    return this.http.put<Profile>(`${this.baseUrl}/me`, payload).pipe(
      tap((profile) => this._me.set(profile)),
      catchError((err) => throwError(() => toApiError(err))),
    );
  }
}
