import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/http/api-error';
import { Azienda, FASCE_PREZZO, FasciaPrezzo, TIPI_AZIENDA, TipoAzienda } from '../../core/models/azienda.models';
import { AziendaService } from '../../core/services/azienda.service';

/** Profilo pubblico di un'azienda (/azienda/:aziendaId): logo, banner, dati e servizi offerti. */
@Component({
  selector: 'app-azienda-profile',
  imports: [RouterLink],
  templateUrl: './azienda-profile.html',
})
export class AziendaProfile {
  private readonly aziendaService = inject(AziendaService);
  private readonly title = inject(Title);

  readonly aziendaId = input.required<string>();

  protected readonly data = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  private readonly tipoLabelByValue = new Map<TipoAzienda, string>(TIPI_AZIENDA.map((t) => [t.value, t.label]));
  private readonly fasciaByValue = new Map<FasciaPrezzo, (typeof FASCE_PREZZO)[number]>(FASCE_PREZZO.map((f) => [f.value, f]));

  protected readonly fascia = computed(() => {
    const value = this.data()?.fasciaPrezzo;
    return value ? (this.fasciaByValue.get(value) ?? null) : null;
  });

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
    effect(() => {
      const nome = this.data()?.nome;
      if (nome) this.title.setTitle(`${nome} — Ristorandoti`);
    });
  }

  protected tipoLabel(tipo: TipoAzienda): string {
    return this.tipoLabelByValue.get(tipo) ?? tipo;
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.data.set(null);
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
