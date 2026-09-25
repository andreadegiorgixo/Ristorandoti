import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { DashboardPermissionService } from '../services/dashboard-permission.service';
import { ToastService } from '../services/toast.service';

/** Riconosce le chiamate a un endpoint della Dashboard aziendale, per riallineare i permessi su un 403. */
const AZIENDA_DASHBOARD_URL = /\/aziende\/(\d+)\/(dashboard|posts\/)/;

/**
 * Aggiunge il Bearer token alle chiamate verso le API Spring Boot.
 * Se una rotta protetta risponde 401 (token scaduto o non valido) chiude la sessione e
 * rimanda al login, che al termine riporta l'utente alla pagina in cui si trovava.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);
  const dashboardPermission = inject(DashboardPermissionService);
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
      // Un permesso può essere stato revocato mentre l'utente è nella Dashboard: riallinea le
      // capability correnti, così il prossimo tentativo (o il prossimo render) mostra già le
      // sezioni in sola lettura invece di un errore confuso.
      if (err instanceof HttpErrorResponse && err.status === 403) {
        const match = AZIENDA_DASHBOARD_URL.exec(req.url);
        if (match) {
          dashboardPermission.refresh(Number(match[1])).subscribe({ error: () => void 0 });
        }
      }
      return throwError(() => err);
    }),
  );
};
