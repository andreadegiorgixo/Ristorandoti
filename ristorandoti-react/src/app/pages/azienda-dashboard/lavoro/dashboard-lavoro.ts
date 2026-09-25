import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';

import { ApiError } from '../../../core/http/api-error';
import { Candidatura, MAX_OFFERTE_ATTIVE, OffertaLavoro } from '../../../core/models/lavoro.models';
import { CandidaturaService } from '../../../core/services/candidatura.service';
import { DashboardPermissionService } from '../../../core/services/dashboard-permission.service';
import { OffertaLavoroService } from '../../../core/services/offerta-lavoro.service';
import { ToastService } from '../../../core/services/toast.service';
import { Avatar } from '../../../shared/components/avatar/avatar';
import { OffertaLavoroFormModal } from '../../../shared/components/offerta-lavoro-form-modal/offerta-lavoro-form-modal';
import { ExpiresInPipe } from '../../../shared/pipes/expires-in.pipe';
import { RelativeTimePipe } from '../../../shared/pipes/relative-time.pipe';

/**
 * Gestione offerte di lavoro: contatore "n/3 attive", pubblicazione con limite (bloccato anche
 * lato client con spiegazione, ma è il backend l'unica fonte di verità), modifica, chiusura e
 * elenco dei candidati per offerta. Sola lettura per chi non ha {@code MANAGE_JOBS} (comunque
 * visibile, come da matrice di default) — anche le offerte scadute restano visibili come storico.
 */
@Component({
  selector: 'app-dashboard-lavoro',
  imports: [OffertaLavoroFormModal, Avatar, RelativeTimePipe, ExpiresInPipe],
  templateUrl: './dashboard-lavoro.html',
})
export class DashboardLavoro {
  private readonly offertaLavoroService = inject(OffertaLavoroService);
  private readonly candidaturaService = inject(CandidaturaService);
  private readonly toast = inject(ToastService);
  protected readonly permissionService = inject(DashboardPermissionService);

  readonly aziendaId = input.required<string>();

  /** Esposto al template per convertire {@link aziendaId} (stringa) in numero senza una pipe dedicata. */
  protected readonly Number = Number;

  protected readonly maxOfferteAttive = MAX_OFFERTE_ATTIVE;
  protected readonly offerte = signal<OffertaLavoro[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly offertaInModifica = signal<OffertaLavoro | null>(null);

  protected readonly espansa = signal<number | null>(null);
  protected readonly candidati = signal<Candidatura[]>([]);
  protected readonly candidatiLoading = signal(false);

  protected readonly puoiScrivere = computed(() => this.permissionService.can('MANAGE_JOBS'));
  protected readonly offerteAttive = computed(() => this.offerte().filter((o) => o.stato === 'ATTIVA'));
  protected readonly puoiPubblicare = computed(() => this.puoiScrivere() && this.offerteAttive().length < this.maxOfferteAttive);

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  protected apriCreazione(): void {
    if (!this.puoiPubblicare()) return;
    this.offertaInModifica.set(null);
    this.formOpen.set(true);
  }

  protected apriModifica(offerta: OffertaLavoro): void {
    this.offertaInModifica.set(offerta);
    this.formOpen.set(true);
  }

  protected onSaved(offerta: OffertaLavoro): void {
    this.formOpen.set(false);
    const inModifica = !!this.offertaInModifica();
    this.offerte.update((list) => (inModifica ? list.map((o) => (o.id === offerta.id ? offerta : o)) : [offerta, ...list]));
    this.toast.success(inModifica ? 'Offerta modificata.' : 'Offerta pubblicata.');
  }

  protected chiudi(offerta: OffertaLavoro): void {
    if (!confirm(`Vuoi chiudere l'offerta "${offerta.titolo}"? I candidati collegati resteranno visibili solo per un periodo limitato.`)) {
      return;
    }
    this.offertaLavoroService.chiudi(Number(this.aziendaId()), offerta.id).subscribe({
      next: () => {
        this.offerte.update((list) => list.filter((o) => o.id !== offerta.id));
        this.toast.success('Offerta chiusa.');
      },
      error: (err: ApiError) => this.toast.error(err.message),
    });
  }

  protected toggleCandidati(offerta: OffertaLavoro): void {
    if (this.espansa() === offerta.id) {
      this.espansa.set(null);
      return;
    }
    this.espansa.set(offerta.id);
    this.candidati.set([]);
    this.candidatiLoading.set(true);
    this.candidaturaService.getCandidati(Number(this.aziendaId()), offerta.id).subscribe({
      next: (pagina) => {
        this.candidati.set(pagina.content);
        this.candidatiLoading.set(false);
      },
      error: (err: ApiError) => {
        this.toast.error(err.message);
        this.candidatiLoading.set(false);
      },
    });
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.offertaLavoroService.getByAziendaDashboard(id, 0, 50).subscribe({
      next: (pagina) => {
        this.offerte.set(pagina.content);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }
}
