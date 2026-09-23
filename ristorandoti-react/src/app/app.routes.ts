import { Routes } from '@angular/router';

import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    title: 'Ristorandoti — La community dei ristoratori',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'about',
    title: 'Chi siamo — Ristorandoti',
    loadComponent: () => import('./pages/about/about').then((m) => m.About),
  },
  {
    path: 'mission',
    title: 'Obiettivo — Ristorandoti',
    loadComponent: () => import('./pages/mission/mission').then((m) => m.Mission),
  },
  {
    path: 'login',
    title: 'Accedi — Ristorandoti',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Registrati — Ristorandoti',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: '**',
    title: 'Pagina non trovata — Ristorandoti',
    loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound),
  },
];
