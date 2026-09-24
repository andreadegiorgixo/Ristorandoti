import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Aggiunge il Bearer token alle chiamate verso le API Spring Boot.
 * Se una rotta protetta risponde 401 (token scaduto o non valido) chiude la sessione e
 * rimanda al login, che al termine riporta l'utente alla pagina in cui si trovava.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);
  const token = auth.token();

  if (!token || !req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((err: unknown) => {
      const isAuthCall = req.url.startsWith(`${environment.apiUrl}/auth/`);
      if (err instanceof HttpErrorResponse && err.status === 401 && !isAuthCall && auth.isAuthenticated()) {
        auth.logout();
        toast.info('La sessione è scaduta: accedi di nuovo per continuare.');
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => err);
    }),
  );
};
