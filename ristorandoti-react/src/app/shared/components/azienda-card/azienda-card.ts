import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ApiError } from '../../../core/http/api-error';
import { Azienda, TIPI_AZIENDA, TipoAzienda } from '../../../core/models/azienda.models';
import { AuthService } from '../../../core/services/auth.service';
import { AziendaService } from '../../../core/services/azienda.service';
import { ToastService } from '../../../core/services/toast.service';
import { AziendaFormModal } from '../azienda-form-modal/azienda-form-modal';

/** Card "Le tue aziende": elenco compatto + pulsante per crearne una nuova (finestra modale). */
@Component({
  selector: 'app-azienda-card',
  imports: [RouterLink, AziendaFormModal],
  templateUrl: './azienda-card.html',
})
export class AziendaCard {
  private readonly auth = inject(AuthService);
  private readonly aziendaService = inject(AziendaService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly aziende = signal<Azienda[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly formOpen = signal(false);

  private readonly labelByTipo = new Map<TipoAzienda, string>(TIPI_AZIENDA.map((t) => [t.value, t.label]));

  constructor() {
    this.load();
  }

  protected tipoLabel(tipo: TipoAzienda): string {
    return this.labelByTipo.get(tipo) ?? tipo;
  }

  protected retry(): void {
    this.load();
  }

  protected onSaved(azienda: Azienda): void {
    this.formOpen.set(false);
    this.toast.success(`"${azienda.nome}" creata!`);
    this.router.navigate(['/azienda', azienda.id]);
  }

  private load(): void {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    this.loading.set(true);
    this.error.set(null);
    this.aziendaService.getByUser(userId, 0).subscribe({
      next: (page) => {
        this.aziende.set(page.content);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }
}
