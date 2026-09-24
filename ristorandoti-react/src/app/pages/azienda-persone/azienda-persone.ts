import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/http/api-error';
import { Azienda, AziendaPersona } from '../../core/models/azienda.models';
import { AziendaService } from '../../core/services/azienda.service';
import { PersonaCard } from '../../shared/components/persona-card/persona-card';
import { InfiniteScrollDirective } from '../../shared/directives/infinite-scroll.directive';

const PAGE_SIZE = 24;

/** Elenco completo delle persone che lavorano in un'azienda (/azienda/:aziendaId/persone). */
@Component({
  selector: 'app-azienda-persone',
  imports: [RouterLink, PersonaCard, InfiniteScrollDirective],
  templateUrl: './azienda-persone.html',
})
export class AziendaPersone {
  private readonly aziendaService = inject(AziendaService);
  private readonly title = inject(Title);

  readonly aziendaId = input.required<string>();

  protected readonly azienda = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly persone = signal<AziendaPersona[]>([]);
  protected readonly personeLoading = signal(false);
  protected readonly personeError = signal<ApiError | null>(null);
  protected readonly last = signal(false);
  protected readonly empty = computed(() => !this.personeLoading() && !this.personeError() && this.persone().length === 0);
  private nextPage = 0;

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
    effect(() => {
      const nome = this.azienda()?.nome;
      if (nome) this.title.setTitle(`Persone di ${nome} — Ristorandoti`);
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  protected loadMore(): void {
    const id = Number(this.aziendaId());
    if (!id || this.personeLoading() || this.last()) return;

    this.personeLoading.set(true);
    this.personeError.set(null);
    this.aziendaService.getPersone(id, this.nextPage, PAGE_SIZE).subscribe({
      next: (page) => {
        const known = new Set(this.persone().map((p) => p.userId));
        this.persone.update((list) => [...list, ...page.content.filter((p) => !known.has(p.userId))]);
        this.last.set(page.last);
        this.nextPage = page.page + 1;
        this.personeLoading.set(false);
      },
      error: (err: ApiError) => {
        this.personeError.set(err);
        this.personeLoading.set(false);
      },
    });
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.azienda.set(null);
    this.persone.set([]);
    this.last.set(false);
    this.personeError.set(null);
    this.nextPage = 0;

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

    this.loadMore();
  }
}
