import { Routes } from '@angular/router';

import { authGuard, landingGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: '',
    title: 'Ristorandoti — La community dei ristoratori',
    canActivate: [landingGuard],
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
    path: 'feed',
    title: 'Feed — Ristorandoti',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/feed/feed').then((m) => m.Feed),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        title: 'Il mio profilo — Ristorandoti',
        loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile),
      },
      {
        path: 'edit',
        title: 'Modifica profilo — Ristorandoti',
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () => import('./pages/profile-edit/profile-edit').then((m) => m.ProfileEdit),
      },
      {
        path: ':userId',
        title: 'Profilo — Ristorandoti',
        loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile),
      },
    ],
  },
  {
    path: 'azienda/:aziendaId',
    title: 'Azienda — Ristorandoti',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/azienda-profile/azienda-profile').then((m) => m.AziendaProfile),
  },
  {
    path: '**',
    title: 'Pagina non trovata — Ristorandoti',
    loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound),
  },
];
