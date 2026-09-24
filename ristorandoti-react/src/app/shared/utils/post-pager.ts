import { computed, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

import { ApiError } from '../../core/http/api-error';
import { Page, Post } from '../../core/models/post.models';

/**
 * Stato di una lista paginata di post (feed o post di un utente): pagine caricate, stato di
 * caricamento, errori, e aggiornamenti locali (nuovo post, like) senza ricaricare la lista.
 */
export class PostPager {
  private readonly _posts = signal<Post[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<ApiError | null>(null);
  private readonly _last = signal(false);
  private readonly _total = signal(0);
  private readonly _loadedOnce = signal(false);
  private nextPage = 0;
  private sub?: Subscription;

  readonly posts = this._posts.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly last = this._last.asReadonly();
  readonly total = this._total.asReadonly();
  /** Primo caricamento in corso: si mostrano gli scheletri al posto della lista. */
  readonly initialLoading = computed(() => this._loading() && !this._loadedOnce());
  readonly empty = computed(() => this._loadedOnce() && !this._error() && this._posts().length === 0);

  constructor(private readonly loader: (page: number) => Observable<Page<Post>>) {}

  /** Riparte dalla prima pagina (primo caricamento, "aggiorna", cambio di utente). */
  reset(): void {
    this.sub?.unsubscribe();
    this.nextPage = 0;
    this._posts.set([]);
    this._last.set(false);
    this._loadedOnce.set(false);
    this._loading.set(false);
    this.loadMore();
  }

  loadMore(): void {
    if (this._loading() || this._last()) return;

    this._loading.set(true);
    this._error.set(null);
    this.sub = this.loader(this.nextPage).subscribe({
      next: (page) => {
        // Evita doppioni se nel frattempo sono stati pubblicati nuovi post (la pagina "scorre")
        const known = new Set(this._posts().map((p) => p.id));
        this._posts.update((list) => [...list, ...page.content.filter((p) => !known.has(p.id))]);
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

  /** Nuovo post pubblicato: compare subito in cima. */
  prepend(post: Post): void {
    this._posts.update((list) => [post, ...list]);
    this._total.update((n) => n + 1);
  }

  /** Aggiorna un post già in lista (es. dopo un like). */
  replace(post: Post): void {
    this._posts.update((list) => list.map((p) => (p.id === post.id ? post : p)));
  }

  destroy(): void {
    this.sub?.unsubscribe();
  }
}
