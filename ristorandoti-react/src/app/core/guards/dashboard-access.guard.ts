import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { DashboardPermissionService } from '../services/dashboard-permission.service';

/**
 * Rotte della Dashboard aziendale (`/azienda/:aziendaId/dashboard/**`): solo chi ha
 * {@code VIEW_DASHBOARD} (o è proprietario) può entrare. Chi non ce l'ha viene rimandato alla
 * vista pubblica, senza errori confusi. L'admin non arriva mai qui automaticamente: ci si arriva
 * solo cliccando "Visualizzala come dashboard" dalla vista pubblica.
 */
export const dashboardAccessGuard: CanActivateFn = (route) => {
  const permissionService = inject(DashboardPermissionService);
  const router = inject(Router);
  const aziendaId = Number(route.paramMap.get('aziendaId'));

  if (!aziendaId) {
    return router.createUrlTree(['/feed']);
  }

  return permissionService.refresh(aziendaId).pipe(
    map((permessi) => (permessi.proprietario || permessi.capabilities.includes('VIEW_DASHBOARD')
      ? true
      : router.createUrlTree(['/azienda', aziendaId]))),
    catchError(() => of(router.createUrlTree(['/azienda', aziendaId]))),
  );
};
