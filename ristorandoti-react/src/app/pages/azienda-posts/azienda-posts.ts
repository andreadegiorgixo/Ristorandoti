import { Component, OnDestroy, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/http/api-error';
import { Azienda } from '../../core/models/azienda.models';
import { AziendaService } from '../../core/services/azienda.service';
import { PostService } from '../../core/services/post.service';
import { PostCard } from '../../shared/components/post-card/post-card';
import { PostSkeleton } from '../../shared/components/post-card/post-skeleton';
import { PostComposer } from '../../shared/components/post-composer/post-composer';
import { InfiniteScrollDirective } from '../../shared/directives/infinite-scroll.directive';
import { PostPager } from '../../shared/utils/post-pager';

/** Elenco completo dei post pubblicati come pagina aziendale (/azienda/:aziendaId/post). */
@Component({
  selector: 'app-azienda-posts',
  imports: [RouterLink, PostCard, PostSkeleton, PostComposer, InfiniteScrollDirective],
  templateUrl: './azienda-posts.html',
})
export class AziendaPosts implements OnDestroy {
  private readonly aziendaService = inject(AziendaService);
  private readonly postService = inject(PostService);
  private readonly title = inject(Title);

  readonly aziendaId = input.required<string>();

  protected readonly azienda = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly pager = new PostPager((page) => this.postService.getByAzienda(Number(this.aziendaId()), page));

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
    });
    effect(() => {
      const nome = this.azienda()?.nome;
      if (nome) this.title.setTitle(`Post di ${nome} — Ristorandoti`);
    });
  }

  ngOnDestroy(): void {
    this.pager.destroy();
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.azienda.set(null);
    this.pager.reset();

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
