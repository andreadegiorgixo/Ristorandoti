import { Component, computed, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../../core/http/api-error';
import { Post } from '../../../core/models/post.models';
import { PostService } from '../../../core/services/post.service';
import { ToastService } from '../../../core/services/toast.service';
import { RelativeTimePipe } from '../../pipes/relative-time.pipe';
import { Avatar } from '../avatar/avatar';

/** Oltre questa lunghezza il testo viene troncato con "Mostra altro". */
const COLLAPSE_AT = 280;

/**
 * Card di un post. Il like è "ottimistico": il cuore si accende subito e, se la chiamata
 * fallisce, torna allo stato precedente con un messaggio d'errore.
 */
@Component({
  selector: 'app-post-card',
  imports: [RouterLink, Avatar, RelativeTimePipe],
  templateUrl: './post-card.html',
  host: { class: 'block' },
})
export class PostCard {
  private readonly postService = inject(PostService);
  private readonly toast = inject(ToastService);

  readonly post = input.required<Post>();
  /** Emesso con il post aggiornato (like): il genitore lo sostituisce nella lista. */
  readonly postChange = output<Post>();

  protected readonly expanded = signal(false);
  protected readonly imageBroken = signal(false);
  private likePending = false;

  protected readonly isLong = computed(() => (this.post().contenuto?.length ?? 0) > COLLAPSE_AT);
  protected readonly text = computed(() => {
    const content = this.post().contenuto ?? '';
    return this.isLong() && !this.expanded() ? `${content.slice(0, COLLAPSE_AT).trimEnd()}…` : content;
  });

  protected toggleLike(): void {
    if (this.likePending) return;
    this.likePending = true;

    const previous = this.post();
    const liked = !previous.likedByMe;
    this.postChange.emit({ ...previous, likedByMe: liked, likeCount: previous.likeCount + (liked ? 1 : -1) });

    const request = liked ? this.postService.like(previous.id) : this.postService.unlike(previous.id);
    request.subscribe({
      next: (updated) => {
        this.likePending = false;
        this.postChange.emit(updated);
      },
      error: (err: ApiError) => {
        this.likePending = false;
        this.postChange.emit(previous);
        this.toast.error(err.message);
      },
    });
  }
}
