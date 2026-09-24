import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Post } from '../../core/models/post.models';
import { AuthService } from '../../core/services/auth.service';
import { PostService } from '../../core/services/post.service';
import { ProfileService } from '../../core/services/profile.service';
import { AziendaCard } from '../../shared/components/azienda-card/azienda-card';
import { Avatar } from '../../shared/components/avatar/avatar';
import { PostCard } from '../../shared/components/post-card/post-card';
import { PostSkeleton } from '../../shared/components/post-card/post-skeleton';
import { PostComposer } from '../../shared/components/post-composer/post-composer';
import { InfiniteScrollDirective } from '../../shared/directives/infinite-scroll.directive';
import { PostPager } from '../../shared/utils/post-pager';

/** Home dell'utente autenticato: card profilo, composer e feed della community con scroll infinito. */
@Component({
  selector: 'app-feed',
  imports: [RouterLink, Avatar, PostCard, PostSkeleton, PostComposer, AziendaCard, InfiniteScrollDirective],
  templateUrl: './feed.html',
})
export class Feed implements OnInit, OnDestroy {
  private readonly postService = inject(PostService);
  protected readonly auth = inject(AuthService);
  protected readonly profile = inject(ProfileService);

  protected readonly pager = new PostPager((page) => this.postService.getFeed(page));

  /** Primo passo mancante per completare il profilo, suggerito nella card laterale. */
  protected readonly nextStep = computed(() => this.profile.completion().steps.find((s) => !s.done));

  ngOnInit(): void {
    this.pager.reset();
  }

  ngOnDestroy(): void {
    this.pager.destroy();
  }

  protected onPublished(post: Post): void {
    this.pager.prepend(post);
  }

  protected refresh(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.pager.reset();
  }
}
