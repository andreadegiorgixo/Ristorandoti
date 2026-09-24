import { Component, OnDestroy, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/http/api-error';
import { Profile as ProfileModel } from '../../core/models/profile.models';
import { AuthService } from '../../core/services/auth.service';
import { FollowService } from '../../core/services/follow.service';
import { PostService } from '../../core/services/post.service';
import { ProfileService } from '../../core/services/profile.service';
import { ToastService } from '../../core/services/toast.service';
import { Avatar } from '../../shared/components/avatar/avatar';
import { PostCard } from '../../shared/components/post-card/post-card';
import { PostSkeleton } from '../../shared/components/post-card/post-skeleton';
import { PostComposer } from '../../shared/components/post-composer/post-composer';
import { ReviewListModal } from '../../shared/components/review-list-modal/review-list-modal';
import { InfiniteScrollDirective } from '../../shared/directives/infinite-scroll.directive';
import { formatPeriod } from '../../shared/utils/dates';
import { PostPager } from '../../shared/utils/post-pager';

type Tab = 'percorso' | 'post';

/**
 * Profilo di un utente (/profile/:userId) o il proprio (/profile). Sul proprio profilo
 * compaiono il pulsante "Modifica" e gli inviti a completare le sezioni vuote.
 */
@Component({
  selector: 'app-profile',
  imports: [RouterLink, Avatar, PostCard, PostSkeleton, PostComposer, ReviewListModal, InfiniteScrollDirective],
  templateUrl: './profile.html',
})
export class Profile implements OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly followService = inject(FollowService);
  private readonly postService = inject(PostService);
  private readonly toast = inject(ToastService);
  private readonly title = inject(Title);

  /** Parametro di rotta (assente su /profile = il mio profilo). */
  readonly userId = input<string>();

  protected readonly targetId = computed(() => Number(this.userId() ?? this.auth.currentUser()?.id));
  protected readonly isOwn = computed(() => this.targetId() === this.auth.currentUser()?.id);

  private readonly otherProfile = signal<ProfileModel | null>(null);
  /** Il proprio profilo arriva dalla cache condivisa: resta aggiornato dopo le modifiche. */
  protected readonly data = computed(() => (this.isOwn() ? this.profileService.me() : this.otherProfile()));
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly tab = signal<Tab>('percorso');
  protected readonly followPending = signal(false);
  protected readonly reviewsOpen = signal(false);

  protected readonly pager = new PostPager((page) => this.postService.getByUser(this.targetId(), page));
  protected readonly formatPeriod = formatPeriod;

  constructor() {
    effect(() => {
      const id = this.targetId();
      untracked(() => this.load(id));
    });
    effect(() => {
      const name = this.data()?.name;
      if (name) this.title.setTitle(`${name} — Ristorandoti`);
    });
  }

  ngOnDestroy(): void {
    this.pager.destroy();
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.otherProfile.set(null);
    this.tab.set('percorso');
    this.reviewsOpen.set(false);
    this.pager.reset();

    const request = this.isOwn() ? this.profileService.getMe() : this.profileService.getByUserId(id);
    request.subscribe({
      next: (profile) => {
        if (!this.isOwn()) this.otherProfile.set(profile);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }

  protected retry(): void {
    this.load(this.targetId());
  }

  protected openReviews(): void {
    this.reviewsOpen.set(true);
  }

  protected closeReviews(): void {
    this.reviewsOpen.set(false);
  }

  /**
   * Segue/smette di seguire il profilo visualizzato. Ottimistico come il like sui post:
   * bottone e contatore si aggiornano subito, e tornano indietro se la chiamata fallisce.
   */
  protected toggleFollow(): void {
    if (this.followPending() || this.isOwn()) return;

    const previous = this.otherProfile();
    if (!previous) return;

    const following = !previous.followedByMe;
    this.followPending.set(true);
    this.otherProfile.set({
      ...previous,
      followedByMe: following,
      followersCount: previous.followersCount + (following ? 1 : -1),
    });

    const request = following
      ? this.followService.follow(previous.userId)
      : this.followService.unfollow(previous.userId);

    request.subscribe({
      next: (status) => {
        this.followPending.set(false);
        this.otherProfile.update((p) => (p ? { ...p, ...status } : p));
      },
      error: (err: ApiError) => {
        this.followPending.set(false);
        this.otherProfile.set(previous);
        this.toast.error(err.message);
      },
    });
  }
}
