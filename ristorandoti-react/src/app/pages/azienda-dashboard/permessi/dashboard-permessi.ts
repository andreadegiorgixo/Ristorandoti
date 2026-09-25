import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import {
  AziendaAuditLogEntry,
  AziendaDipendenteRuoli,
  AziendaRuoloCatalogo,
  AziendaRuoloCodice,
  CAPABILITY_LABEL,
  RUOLI_ASSEGNABILI,
} from '../../../core/models/dashboard.models';
import { DashboardPermissionService } from '../../../core/services/dashboard-permission.service';
import { ToastService } from '../../../core/services/toast.service';
import { Avatar } from '../../../shared/components/avatar/avatar';
import { RelativeTimePipe } from '../../../shared/pipes/relative-time.pipe';

/**
 * Gestione permessi: elenco dei dipendenti attualmente assunti con i ruoli attivi, assegnazione e
 * revoca (solo per chi ha {@code MANAGE_PERMISSIONS}), legenda delle capability per ruolo e log di
 * audit. Sola lettura per tutti gli altri (comunque visibile, come da matrice di default).
 */
@Component({
  selector: 'app-dashboard-permessi',
  imports: [FormsModule, Avatar, RelativeTimePipe],
  templateUrl: './dashboard-permessi.html',
})
export class DashboardPermessi {
  private readonly permissionService = inject(DashboardPermissionService);
  private readonly toast = inject(ToastService);

  readonly aziendaId = input.required<string>();

  /** Esposto al template per convertire {@link aziendaId} (stringa) in numero senza una pipe dedicata. */
  protected readonly Number = Number;

  protected readonly ruoliAssegnabili = RUOLI_ASSEGNABILI;
  protected readonly capabilityLabel = CAPABILITY_LABEL;

  protected readonly puoiGestire = signal(false);
  protected readonly catalogo = signal<AziendaRuoloCatalogo[]>([]);
  protected readonly dipendenti = signal<AziendaDipendenteRuoli[]>([]);
  protected readonly audit = signal<AziendaAuditLogEntry[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly ruoloSceltoPerUtente = signal<Record<number, AziendaRuoloCodice>>({});
  protected readonly azioneInCorso = signal<number | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  protected ruoloScelto(userId: number): AziendaRuoloCodice {
    return this.ruoloSceltoPerUtente()[userId] ?? 'HR';
  }

  protected impostaRuoloScelto(userId: number, ruolo: AziendaRuoloCodice): void {
    this.ruoloSceltoPerUtente.update((mappa) => ({ ...mappa, [userId]: ruolo }));
  }

  protected assegna(id: number, dipendente: AziendaDipendenteRuoli): void {
    const ruolo = this.ruoloScelto(dipendente.userId);
    if (ruolo === 'ADMIN' && !confirm(`Vuoi promuovere ${dipendente.name} ad Admin? Avrà accesso completo alla Dashboard.`)) {
      return;
    }
    this.azioneInCorso.set(dipendente.userId);
    this.permissionService.assegna(id, dipendente.userId, ruolo).subscribe({
      next: () => {
        this.azioneInCorso.set(null);
        this.toast.success(`Ruolo assegnato a ${dipendente.name}.`);
        this.load(id);
      },
      error: (err: ApiError) => {
        this.azioneInCorso.set(null);
        this.toast.error(err.message);
      },
    });
  }

  protected revoca(id: number, dipendente: AziendaDipendenteRuoli, ruolo: AziendaRuoloCodice): void {
    const messaggio =
      ruolo === 'ADMIN'
        ? `Vuoi rimuovere il ruolo Admin a ${dipendente.name}?`
        : `Vuoi revocare il ruolo ${this.nomeRuolo(ruolo)} a ${dipendente.name}?`;
    if (!confirm(messaggio)) return;

    this.azioneInCorso.set(dipendente.userId);
    this.permissionService.revoca(id, dipendente.userId, ruolo).subscribe({
      next: () => {
        this.azioneInCorso.set(null);
        this.toast.success(`Ruolo revocato a ${dipendente.name}.`);
        this.load(id);
      },
      error: (err: ApiError) => {
        this.azioneInCorso.set(null);
        this.toast.error(err.message);
      },
    });
  }

  protected nomeRuolo(codice: AziendaRuoloCodice): string {
    return this.ruoliAssegnabili.find((r) => r.value === codice)?.label ?? codice;
  }

  protected azioneLabel(entry: AziendaAuditLogEntry): string {
    switch (entry.azione) {
      case 'ASSEGNATO':
        return `ha assegnato ${this.nomeRuolo(entry.ruoloCodice)} a`;
      case 'REVOCATO':
        return `ha revocato ${this.nomeRuolo(entry.ruoloCodice)} a`;
      case 'REVOCATO_FINE_RAPPORTO':
        return `${this.nomeRuolo(entry.ruoloCodice)} revocato automaticamente (fine rapporto) a`;
    }
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);

    this.permissionService.catalogoRuoli(id).subscribe({
      next: (catalogo) => this.catalogo.set(catalogo),
      error: () => this.catalogo.set([]),
    });

    this.permissionService.dipendenti(id).subscribe({
      next: (dipendenti) => {
        this.dipendenti.set(dipendenti);
        this.puoiGestire.set(true);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        // 403 per chi non ha MANAGE_PERMISSIONS: sezione visibile ma in sola lettura, non un errore.
        if (err.status === 403) {
          this.puoiGestire.set(false);
          this.dipendenti.set([]);
          this.loading.set(false);
          return;
        }
        this.error.set(err);
        this.loading.set(false);
      },
    });

    this.permissionService.audit(id, 0, 20).subscribe({
      next: (pagina) => this.audit.set(pagina.content),
      error: () => this.audit.set([]),
    });
  }
}
