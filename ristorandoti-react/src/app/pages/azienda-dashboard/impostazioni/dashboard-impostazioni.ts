import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';

import { ApiError } from '../../../core/http/api-error';
import { Azienda } from '../../../core/models/azienda.models';
import { AziendaService } from '../../../core/services/azienda.service';
import { DashboardPermissionService } from '../../../core/services/dashboard-permission.service';
import { ToastService } from '../../../core/services/toast.service';
import { AziendaFormModal } from '../../../shared/components/azienda-form-modal/azienda-form-modal';

/**
 * Impostazioni pagina (dati anagrafici, logo, copertina): qui vive oggi la funzione un tempo
 * raggiungibile dal pulsante "Modifica" sulla vista pubblica, ora spostata nella Dashboard.
 * Non essendo una delle 5 capability del brief, l'accesso in scrittura è riservato al proprietario
 * o a chi ha {@code MANAGE_PERMISSIONS} (tier Admin), la più vicina delle capability esistenti.
 */
@Component({
  selector: 'app-dashboard-impostazioni',
  imports: [AziendaFormModal],
  templateUrl: './dashboard-impostazioni.html',
})
export class DashboardImpostazioni {
  private readonly aziendaService = inject(AziendaService);
  private readonly toast = inject(ToastService);
  protected readonly permissionService = inject(DashboardPermissionService);

  readonly aziendaId = input.required<string>();

  protected readonly data = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly editOpen = signal(false);

  protected readonly puoiModificare = computed(
    () => (this.permissionService.permessi()?.proprietario ?? false) || this.permissionService.can('MANAGE_PERMISSIONS'),
  );

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  protected onSaved(azienda: Azienda): void {
    this.data.set(azienda);
    this.editOpen.set(false);
    this.toast.success('Modifiche salvate.');
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);

    this.aziendaService.getById(id).subscribe({
      next: (azienda) => {
        this.data.set(azienda);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }
}
