import { Component, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, forkJoin, map, of, switchMap } from 'rxjs';

import { Azienda, TIPI_AZIENDA, TipoAzienda } from '../../../core/models/azienda.models';
import { OffertaLavoro } from '../../../core/models/lavoro.models';
import { PersonaSearchResult } from '../../../core/models/profile.models';
import { AziendaService } from '../../../core/services/azienda.service';
import { OffertaLavoroService } from '../../../core/services/offerta-lavoro.service';
import { ProfileService } from '../../../core/services/profile.service';
import { Avatar } from '../avatar/avatar';

/** Sotto questa lunghezza non si cerca ancora (evita chiamate inutili). */
const MIN_LENGTH = 2;
const RESULTS_PER_GROUP = 5;

interface GroupedResults {
  persone: PersonaSearchResult[];
  aziende: Azienda[];
  lavoro: OffertaLavoro[];
}

const EMPTY_RESULTS: GroupedResults = { persone: [], aziende: [], lavoro: [] };

/**
 * Barra di ricerca globale in navbar: cerca contemporaneamente persone, aziende e offerte di
 * lavoro (tipo-mentre-scrivi con debounce, stesso pattern RxJS della ricerca azienda in
 * profile-edit.ts, esteso a tre fonti in parallelo con {@link forkJoin}).
 */
@Component({
  selector: 'app-global-search',
  imports: [RouterLink, Avatar],
  templateUrl: './global-search.html',
})
export class GlobalSearch {
  /** Un risultato è stato selezionato: la navbar la usa per chiudere anche il menu mobile. */
  readonly selected = output<void>();

  private readonly router = inject(Router);
  private readonly profileService = inject(ProfileService);
  private readonly aziendaService = inject(AziendaService);
  private readonly offertaLavoroService = inject(OffertaLavoroService);

  private readonly tipoLabelByValue = new Map<TipoAzienda, string>(TIPI_AZIENDA.map((t) => [t.value, t.label]));

  protected readonly query = signal('');
  protected readonly open = signal(false);
  protected readonly loading = signal(false);
  protected readonly results = signal<GroupedResults>(EMPTY_RESULTS);

  private readonly query$ = new Subject<string>();

  private readonly querySub = this.query$
    .pipe(
      debounceTime(300),
      switchMap((term) => {
        const trimmed = term.trim();
        if (trimmed.length < MIN_LENGTH) {
          this.loading.set(false);
          return of(EMPTY_RESULTS);
        }
        this.loading.set(true);
        return forkJoin({
          persone: this.profileService.search(trimmed, 0, RESULTS_PER_GROUP).pipe(
            map((page) => page.content),
            catchError(() => of([] as PersonaSearchResult[])),
          ),
          aziende: this.aziendaService.search(trimmed, 0, RESULTS_PER_GROUP).pipe(
            map((page) => page.content),
            catchError(() => of([] as Azienda[])),
          ),
          lavoro: this.offertaLavoroService.search(trimmed, 0, RESULTS_PER_GROUP).pipe(
            map((page) => page.content),
            catchError(() => of([] as OffertaLavoro[])),
          ),
        });
      }),
      takeUntilDestroyed(),
    )
    .subscribe((grouped) => {
      this.loading.set(false);
      this.results.set(grouped);
    });

  protected readonly minLength = MIN_LENGTH;

  protected tipoLabel(tipo: TipoAzienda): string {
    return this.tipoLabelByValue.get(tipo) ?? tipo;
  }

  protected onInput(value: string): void {
    this.query.set(value);
    this.open.set(true);
    this.query$.next(value);
  }

  protected onFocus(): void {
    if (this.query().trim()) this.open.set(true);
  }

  protected close(): void {
    this.open.set(false);
  }

  protected selectResult(): void {
    this.close();
    this.selected.emit();
  }

  /** Invio nella barra, o click su "Vedi tutti i risultati": apre la pagina risultati completa. */
  protected goToResults(): void {
    const trimmed = this.query().trim();
    if (trimmed.length < MIN_LENGTH) return;
    this.close();
    this.selected.emit();
    this.router.navigate(['/ricerca'], { queryParams: { q: trimmed } });
  }
}
