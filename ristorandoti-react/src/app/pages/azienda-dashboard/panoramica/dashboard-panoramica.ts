import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { OVERVIEW_MAX_LENGTH } from '../../../core/config/dashboard.config';
import { ApiError } from '../../../core/http/api-error';
import { Azienda } from '../../../core/models/azienda.models';
import { AziendaService } from '../../../core/services/azienda.service';
import { DashboardPermissionService } from '../../../core/services/dashboard-permission.service';
import { ToastService } from '../../../core/services/toast.service';
import { OverviewTextPipe } from '../../../shared/pipes/overview-text.pipe';

/**
 * Editor della sezione Panoramica (descrizione dell'azienda, mostrata anche nella vista pubblica
 * tra i dati della pagina e i post). Sola lettura per chi non ha {@code MANAGE_OVERVIEW}
 * (default: solo Admin).
 */
@Component({
  selector: 'app-dashboard-panoramica',
  imports: [FormsModule, OverviewTextPipe],
  templateUrl: './dashboard-panoramica.html',
})
export class DashboardPanoramica {
  private readonly aziendaService = inject(AziendaService);
  private readonly toast = inject(ToastService);
  protected readonly permissionService = inject(DashboardPermissionService);

  readonly aziendaId = input.required<string>();

  protected readonly maxLunghezza = OVERVIEW_MAX_LENGTH;
  protected readonly azienda = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly testo = signal('');
  protected readonly saving = signal(false);
  protected readonly modificaAttiva = signal(false);

  protected readonly puoiScrivere = computed(() => this.permissionService.can('MANAGE_OVERVIEW'));

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  protected iniziaModifica(): void {
    this.testo.set(this.azienda()?.descrizione ?? '');
    this.modificaAttiva.set(true);
  }

  protected annulla(): void {
    this.modificaAttiva.set(false);
  }

  protected salva(): void {
    this.saving.set(true);
    this.aziendaService.updatePanoramica(Number(this.aziendaId()), this.testo().trim()).subscribe({
      next: (azienda) => {
        this.saving.set(false);
        this.azienda.set(azienda);
        this.modificaAttiva.set(false);
        this.toast.success('Panoramica aggiornata.');
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.toast.error(err.message);
      },
    });
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.aziendaService.getById(id).subscribe({
      next: (azienda) => {
        this.azienda.set(azienda);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }
}
