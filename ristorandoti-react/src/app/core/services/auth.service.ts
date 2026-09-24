import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApiErrorResponse,
  AuthError,
  AuthResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest,
} from '../models/auth.models';

const TOKEN_KEY = 'ristorandoti.token';
const USER_KEY = 'ristorandoti.user';
const EXPIRES_AT_KEY = 'ristorandoti.expiresAt';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly _token = signal<string | null>(null);
  private readonly _user = signal<AuthUser | null>(null);

  readonly token = this._token.asReadonly();
  readonly currentUser = this._user.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());

  private logoutTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.restoreSession();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials).pipe(
      tap((res) => this.storeSession(res)),
      catchError((err: HttpErrorResponse) => throwError(() => this.toAuthError(err))),
    );
  }

  /** Il backend autentica subito il nuovo utente: la risposta contiene già il JWT. */
  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, payload).pipe(
      tap((res) => this.storeSession(res)),
      catchError((err: HttpErrorResponse) => throwError(() => this.toAuthError(err))),
    );
  }

  /** Scambia l'ID token di Google con il JWT applicativo (crea l'account se non esiste). */
  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/google`, { idToken }).pipe(
      tap((res) => this.storeSession(res)),
      catchError((err: HttpErrorResponse) => throwError(() => this.toAuthError(err))),
    );
  }

  logout(): void {
    clearTimeout(this.logoutTimer);
    [TOKEN_KEY, USER_KEY, EXPIRES_AT_KEY].forEach((key) => this.remove(key));
    this._token.set(null);
    this._user.set(null);
  }

  private storeSession(res: AuthResponse): void {
    const user: AuthUser = { id: res.id, name: res.name, email: res.email, roles: res.roles ?? [] };
    const expiresAt = Date.now() + res.expiresIn * 1000;

    this.write(TOKEN_KEY, res.token);
    this.write(USER_KEY, JSON.stringify(user));
    this.write(EXPIRES_AT_KEY, String(expiresAt));
    this.applySession(res.token, user, expiresAt);
  }

  private restoreSession(): void {
    const token = this.read(TOKEN_KEY);
    const expiresAt = Number(this.read(EXPIRES_AT_KEY));
    const user = this.readUser();

    if (token && user && expiresAt > Date.now()) {
      this.applySession(token, user, expiresAt);
    } else {
      this.logout();
    }
  }

  /** Il JWT scade dopo expiresIn secondi: a scadenza la sessione viene chiusa. */
  private applySession(token: string, user: AuthUser, expiresAt: number): void {
    this._token.set(token);
    this._user.set(user);
    clearTimeout(this.logoutTimer);
    this.logoutTimer = setTimeout(() => this.logout(), expiresAt - Date.now());
  }

  private toAuthError(err: HttpErrorResponse): AuthError {
    const body = err.error as Partial<ApiErrorResponse> | null;
    const message = body?.message;

    switch (err.status) {
      case 0:
        return {
          code: 'SERVER_UNREACHABLE',
          message: 'Impossibile contattare il server. Riprova tra qualche istante.',
        };
      case 401:
        return { code: 'INVALID_CREDENTIALS', message: message ?? 'Email o password non corretti.' };
      // Solo su /login: l'email non corrisponde a nessun account
      case 404:
        return { code: 'NOT_REGISTERED', message: message ?? 'Nessun account registrato con questa email.' };
      case 409:
        return { code: 'ALREADY_EXISTS', message: message ?? 'Esiste già un account con questa email.' };
      case 400:
        return {
          code: 'VALIDATION',
          message: message ?? 'Alcuni dati non sono validi.',
          fieldErrors: body?.fieldErrors,
        };
      default:
        return { code: 'UNKNOWN', message: message ?? 'Si è verificato un errore imprevisto. Riprova.' };
    }
  }

  // localStorage può essere assente o bloccato (SSR, navigazione privata)
  private read(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private readUser(): AuthUser | null {
    const raw = this.read(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      /* storage non disponibile: la sessione resta solo in memoria */
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      /* noop */
    }
  }
}
