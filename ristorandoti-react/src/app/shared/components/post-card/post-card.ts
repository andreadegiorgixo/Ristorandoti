import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../../core/http/api-error';
import { Post } from '../../../core/models/post.models';
import { AuthService } from '../../../core/services/auth.service';
import { PostService } from '../../../core/services/post.service';
import { ToastService } from '../../../core/services/toast.service';
import { RelativeTimePipe } from '../../pipes/relative-time.pipe';
import { MAX_POST_LENGTH } from '../post-composer/post-composer';
import { Avatar } from '../avatar/avatar';

/** Oltre questa lunghezza il testo viene troncato con "Mostra altro". */
const COLLAPSE_AT = 280;

/**
 * Card di un post. Il like è "ottimistico": il cuore si accende subito e, se la chiamata
 * fallisce, torna allo stato precedente con un messaggio d'errore.
 */
@Component({
  selector: 'app-post-card',
  imports: [RouterLink, FormsModule, Avatar, RelativeTimePipe],
  templateUrl: './post-card.html',
  host: { class: 'block' },
})
export class PostCard {
  private readonly postService = inject(PostService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  readonly post = input.required<Post>();
  /** {@code true} solo nei contesti di post personali (feed, proprio profilo): abilita la modifica del proprio post. */
  readonly editable = input(false);
  /** Emesso con il post aggiornato (like, modifica): il genitore lo sostituisce nella lista. */
  readonly postChange = output<Post>();

  protected readonly expanded = signal(false);
  protected readonly imageBroken = signal(false);
  private likePending = false;

  protected readonly canEdit = computed(() => this.editable() && this.post().autoreId === this.auth.currentUser()?.id);
  protected readonly isEditing = signal(false);
  protected readonly editText = signal('');
  protected readonly saving = signal(false);
  protected readonly maxLength = MAX_POST_LENGTH;

  protected startEdit(): void {
    this.editText.set(this.post().contenuto ?? '');
    this.isEditing.set(true);
  }

  protected cancelEdit(): void {
    this.isEditing.set(false);
  }

  protected saveEdit(): void {
    const post = this.post();
    const testo = this.editText().trim();
    if (!testo && !post.mediaUrl) return; // un post non può restare del tutto vuoto

    this.saving.set(true);
    this.postService.update(post.id, { contenuto: testo || undefined, mediaUrl: post.mediaUrl ?? undefined }).subscribe({
      next: (aggiornato) => {
        this.saving.set(false);
        this.isEditing.set(false);
        this.postChange.emit(aggiornato);
        this.toast.success('Post modificato.');
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.toast.error(err.message);
      },
    });
  }

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
