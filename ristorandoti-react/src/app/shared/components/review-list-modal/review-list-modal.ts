import { Component, HostListener, effect, inject, input, output, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../../core/http/api-error';
import { Review } from '../../../core/models/review.models';
import { ReviewService } from '../../../core/services/review.service';
import { RelativeTimePipe } from '../../pipes/relative-time.pipe';
import { Avatar } from '../avatar/avatar';

/** Finestra modale con le recensioni ricevute da un utente da parte di colleghi/ex colleghi. */
@Component({
  selector: 'app-review-list-modal',
  imports: [RouterLink, Avatar, RelativeTimePipe],
  templateUrl: './review-list-modal.html',
})
export class ReviewListModal {
  private readonly reviewService = inject(ReviewService);

  readonly userId = input.required<number>();
  readonly userName = input<string>('');
  readonly closed = output<void>();

  protected readonly stars = [1, 2, 3, 4, 5];

  protected readonly reviews = signal<Review[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadingMore = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly page = signal(0);
  protected readonly last = signal(true);
  protected readonly total = signal(0);

  constructor() {
    effect(() => {
      const id = this.userId();
      untracked(() => this.loadPage(id, 0));
    });
  }

  @HostListener('document:keydown.escape')
  protected close(): void {
    this.closed.emit();
  }

  protected loadMore(): void {
    if (this.loadingMore() || this.last()) return;
    this.loadPage(this.userId(), this.page() + 1);
  }

  protected retry(): void {
    this.loadPage(this.userId(), this.page());
  }

  private loadPage(userId: number, page: number): void {
    this.error.set(null);
    if (page === 0) this.loading.set(true);
    else this.loadingMore.set(true);

    this.reviewService.getForUser(userId, page).subscribe({
      next: (result) => {
        this.reviews.update((list) => (page === 0 ? result.content : [...list, ...result.content]));
        this.page.set(result.page);
        this.last.set(result.last);
        this.total.set(result.totalElements);
        this.loading.set(false);
        this.loadingMore.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
        this.loadingMore.set(false);
      },
    });
  }
}
