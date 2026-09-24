import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/** Rotte riservate agli utenti autenticati: gli altri vanno al login e poi tornano qui. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isAuthenticated()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Sulla landing page: chi è già autenticato va direttamente al feed. */
export const landingGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/feed']) : true;
};

/**
 * Pagina in cui portare l'utente dopo login/registrazione: il `returnUrl` impostato da
 * {@link authGuard}, se è un percorso interno; altrimenti il feed.
 * Si scartano gli URL esterni ("//sito.it", "https://…") per evitare open redirect.
 */
export function afterLoginUrl(returnUrl: string | null | undefined): string {
  const internal = !!returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') && !returnUrl.startsWith('/\\');
  return internal ? returnUrl! : '/feed';
}
