import { Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Capability } from '../../core/models/dashboard.models';
import { DashboardPermissionService } from '../../core/services/dashboard-permission.service';

interface NavItem {
  path: string;
  label: string;
  /** {@code null} = sempre in sola lettura per tutti (Home); altrimenti la capability che la rende scrivibile. */
  capability: Capability | null;
}

const NAV_ITEMS: NavItem[] = [
  { path: 'home', label: 'Home', capability: null },
  { path: 'post', label: 'Post', capability: 'MANAGE_POSTS' },
  { path: 'lavoro', label: 'Lavoro', capability: 'MANAGE_JOBS' },
  { path: 'panoramica', label: 'Panoramica', capability: 'MANAGE_OVERVIEW' },
  { path: 'permessi', label: 'Permessi', capability: 'MANAGE_PERMISSIONS' },
  { path: 'impostazioni', label: 'Impostazioni pagina', capability: 'MANAGE_PERMISSIONS' },
];

/**
 * Shell della Dashboard aziendale (/azienda/:aziendaId/dashboard/**): navigazione tra le sezioni
 * e punto di ritorno alla vista pubblica ("Visualizza come utente"). Tutte le voci restano sempre
 * visibili: chi non ha la capability corrispondente vede comunque la sezione, ma in sola lettura
 * (indicatore esplicito qui in nav, oltre a quello dentro ciascuna sezione).
 */
@Component({
  selector: 'app-azienda-dashboard-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './azienda-dashboard-shell.html',
})
export class AziendaDashboardShell {
  protected readonly permissionService = inject(DashboardPermissionService);

  readonly aziendaId = input.required<string>();

  protected readonly navItems = NAV_ITEMS;

  protected readonly proprietario = computed(() => this.permissionService.permessi()?.proprietario ?? false);

  protected puoiScrivere(capability: Capability | null): boolean {
    if (capability === null) return false;
    return this.proprietario() || this.permissionService.can(capability);
  }
}
