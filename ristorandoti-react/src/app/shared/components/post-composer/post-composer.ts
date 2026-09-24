import { Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { Azienda } from '../../../core/models/azienda.models';
import { Post } from '../../../core/models/post.models';
import { AuthService } from '../../../core/services/auth.service';
import { PostService } from '../../../core/services/post.service';
import { ProfileService } from '../../../core/services/profile.service';
import { ToastService } from '../../../core/services/toast.service';
import { IMAGE_ACCEPT, UploadService } from '../../../core/services/upload.service';
import { Avatar } from '../avatar/avatar';

/** Limite di CreatePostRequestDto.contenuto */
export const MAX_POST_LENGTH = 3000;

/**
 * Box "Cosa vuoi condividere?" in cima al feed. Resta compatto finché non ci si clicca,
 * poi si espande; Ctrl/⌘ + Invio pubblica.
 *
 * <p>Se si valorizza {@link azienda}, pubblica come pagina aziendale invece che a nome
 * dell'utente autenticato (usato dalla tab LAVORO/HOME del profilo aziendale, solo per chi può
 * gestirlo: il controllo è comunque applicato anche lato backend).</p>
 */
@Component({
  selector: 'app-post-composer',
  imports: [ReactiveFormsModule, Avatar],
  templateUrl: './post-composer.html',
  host: { class: 'block' },
})
export class PostComposer {
  private readonly postService = inject(PostService);
  private readonly uploadService = inject(UploadService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);
  protected readonly profile = inject(ProfileService);

  /** Azienda per cui pubblicare; assente per un post personale. */
  readonly azienda = input<Azienda | null>(null);

  readonly published = output<Post>();

  private readonly textarea = viewChild<ElementRef<HTMLTextAreaElement>>('textarea');
  private readonly fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  protected readonly maxLength = MAX_POST_LENGTH;
  protected readonly imageAccept = IMAGE_ACCEPT;
  protected readonly expanded = signal(false);
  protected readonly uploading = signal(false);
  protected readonly publishing = signal(false);

  protected readonly form = new FormGroup({
    contenuto: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(MAX_POST_LENGTH)] }),
    /** URL restituito da POST /api/uploads/images dopo aver scelto il file */
    mediaUrl: new FormControl('', { nonNullable: true }),
  });

  /** Foto e nome mostrati nell'intestazione: dell'azienda se si pubblica come pagina, altrimenti i propri. */
  protected readonly avatarSrc = computed(() => this.azienda()?.fotoProfiloUrl ?? this.profile.me()?.profilePictureUrl);
  protected readonly displayName = computed(() => this.azienda()?.nome ?? this.auth.currentUser()?.name ?? '');

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly length = computed(() => this.value().contenuto?.length ?? 0);
  protected readonly mediaPreview = computed(() => this.value().mediaUrl || null);
  protected readonly canPublish = computed(() => {
    const v = this.value();
    const hasContent = !!v.contenuto?.trim() || !!this.mediaPreview();
    return hasContent && this.length() <= MAX_POST_LENGTH && !this.publishing() && !this.uploading();
  });

  protected expand(): void {
    this.expanded.set(true);
    queueMicrotask(() => this.textarea()?.nativeElement.focus());
  }

  /** Apre la finestra "scegli file" del sistema operativo. */
  protected pickImage(): void {
    if (this.uploading()) return;
    this.expanded.set(true);
    // Nel box compatto l'input non esiste ancora: si attende il render del form
    queueMicrotask(() => this.fileInput()?.nativeElement.click());
  }

  protected onFileSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = ''; // permette di riscegliere lo stesso file dopo averlo rimosso
    if (!file) return;

    this.uploading.set(true);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.uploading.set(false);
        this.form.controls.mediaUrl.setValue(url);
      },
      error: (err: ApiError) => {
        this.uploading.set(false);
        this.toast.error(err.message);
      },
    });
  }

  protected removeImage(): void {
    this.form.controls.mediaUrl.setValue('');
  }

  /** La textarea cresce con il testo, senza barra di scorrimento. */
  protected autoResize(el: HTMLTextAreaElement): void {
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight}px`;
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault();
      this.publish();
    }
  }

  protected cancel(): void {
    this.form.reset();
    this.expanded.set(false);
  }

  protected publish(): void {
    if (!this.canPublish()) return;

    const { contenuto, mediaUrl } = this.form.getRawValue();
    const payload = { contenuto: contenuto.trim() || undefined, mediaUrl: mediaUrl || undefined };
    const azienda = this.azienda();
    this.publishing.set(true);

    (azienda ? this.postService.createForAzienda(azienda.id, payload) : this.postService.create(payload)).subscribe({
        next: (post) => {
          this.publishing.set(false);
          this.cancel();
          this.published.emit(post);
          this.toast.success('Post pubblicato!');
        },
        error: (err: ApiError) => {
          this.publishing.set(false);
          this.toast.error(err.message);
        },
      });
  }
}
