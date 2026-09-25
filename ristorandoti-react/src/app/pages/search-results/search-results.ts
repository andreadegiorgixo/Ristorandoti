import { Component, OnDestroy, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { TIPI_AZIENDA, TipoAzienda } from '../../core/models/azienda.models';
import { AziendaService } from '../../core/services/azienda.service';
import { OffertaLavoroService } from '../../core/services/offerta-lavoro.service';
import { ProfileService } from '../../core/services/profile.service';
import { Avatar } from '../../shared/components/avatar/avatar';
import { InfiniteScrollDirective } from '../../shared/directives/infinite-scroll.directive';
import { ResultPager } from '../../shared/utils/result-pager';

type Tab = 'persone' | 'aziende' | 'lavoro';

/**
 * Pagina risultati della ricerca globale ({@code /ricerca?q=...}): tre tab (Persone, Aziende,
 * Lavoro), ciascuna con scroll infinito. Complementare al dropdown di {@code app-global-search}
 * in navbar, che mostra solo i primi risultati di ciascuna categoria.
 */
@Component({
  selector: 'app-search-results',
  imports: [RouterLink, Avatar, InfiniteScrollDirective],
  templateUrl: './search-results.html',
})
export class SearchResults implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly profileService = inject(ProfileService);
  private readonly aziendaService = inject(AziendaService);
  private readonly offertaLavoroService = inject(OffertaLavoroService);
  private readonly title = inject(Title);

  private readonly tipoLabelByValue = new Map<TipoAzienda, string>(TIPI_AZIENDA.map((t) => [t.value, t.label]));

  protected readonly query = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('q') ?? '')), {
    initialValue: this.route.snapshot.queryParamMap.get('q') ?? '',
  });

  protected readonly tab = signal<Tab>('persone');

  protected readonly persone = new ResultPager(
    (page) => this.profileService.search(this.query(), page, 20),
    (p) => p.userId,
  );
  protected readonly aziende = new ResultPager(
    (page) => this.aziendaService.search(this.query(), page, 20),
    (a) => a.id,
  );
  protected readonly lavoro = new ResultPager(
    (page) => this.offertaLavoroService.search(this.query(), page, 20),
    (o) => o.id,
  );

  constructor() {
    effect(() => {
      const q = this.query();
      untracked(() => {
        this.title.setTitle(q ? `“${q}” — Ricerca — Ristorandoti` : 'Ricerca — Ristorandoti');
        this.tab.set('persone');
        this.persone.reset();
        this.aziende.reset();
        this.lavoro.reset();
      });
    });
  }

  ngOnDestroy(): void {
    this.persone.destroy();
    this.aziende.destroy();
    this.lavoro.destroy();
  }

  protected tipoLabel(tipo: TipoAzienda): string {
    return this.tipoLabelByValue.get(tipo) ?? tipo;
  }

  protected readonly hasAnyQuery = computed(() => this.query().trim().length > 0);
}
