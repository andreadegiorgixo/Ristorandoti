import { computed, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

import { ApiError } from '../../core/http/api-error';
import { Page } from '../../core/models/post.models';

/**
 * Stato di una lista paginata generica con scroll infinito, sullo stesso modello di
 * {@link import('./post-pager').PostPager} ma senza le specificità di {@code Post}
 * (prepend/replace): usata dalle tab della pagina risultati di ricerca.
 */
export class ResultPager<T> {
  private readonly _items = signal<T[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<ApiError | null>(null);
  private readonly _last = signal(false);
  private readonly _total = signal(0);
  private readonly _loadedOnce = signal(false);
  private nextPage = 0;
  private sub?: Subscription;

  readonly items = this._items.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly last = this._last.asReadonly();
  readonly total = this._total.asReadonly();
  readonly initialLoading = computed(() => this._loading() && !this._loadedOnce());
  readonly empty = computed(() => this._loadedOnce() && !this._error() && this._items().length === 0);

  constructor(
    private readonly loader: (page: number) => Observable<Page<T>>,
    private readonly idOf: (item: T) => number | string,
  ) {}

  /** Riparte dalla prima pagina (nuova ricerca). */
  reset(): void {
    this.sub?.unsubscribe();
    this.nextPage = 0;
    this._items.set([]);
    this._last.set(false);
    this._loadedOnce.set(false);
    this._loading.set(false);
    this._error.set(null);
    this.loadMore();
  }

  loadMore(): void {
    if (this._loading() || this._last()) return;

    this._loading.set(true);
    this._error.set(null);
    this.sub = this.loader(this.nextPage).subscribe({
      next: (page) => {
        const known = new Set(this._items().map(this.idOf));
        this._items.update((list) => [...list, ...page.content.filter((item) => !known.has(this.idOf(item)))]);
        this._last.set(page.last);
        this._total.set(page.totalElements);
        this.nextPage = page.page + 1;
        this._loadedOnce.set(true);
        this._loading.set(false);
      },
      error: (err: ApiError) => {
        this._error.set(err);
        this._loadedOnce.set(true);
        this._loading.set(false);
      },
    });
  }

  destroy(): void {
    this.sub?.unsubscribe();
  }
}
