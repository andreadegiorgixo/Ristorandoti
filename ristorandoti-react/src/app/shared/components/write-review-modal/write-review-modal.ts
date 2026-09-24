import { Component, HostListener, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { Review } from '../../../core/models/review.models';
import { ReviewService } from '../../../core/services/review.service';
import { ToastService } from '../../../core/services/toast.service';

const MAX_TESTO_LENGTH = 2000;

/**
 * Finestra modale per lasciare o modificare la recensione dell'utente autenticato verso un
 * collega/ex collega. Chi la apre ha già superato il controllo di eleggibilità
 * (ReviewEligibilityDto.canReview) lato chiamante: qui ci si limita a inviare la richiesta,
 * che resta comunque validata anche server-side.
 */
@Component({
  selector: 'app-write-review-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './write-review-modal.html',
})
export class WriteReviewModal {
  private readonly reviewService = inject(ReviewService);
  private readonly toast = inject(ToastService);

  readonly userId = input.required<number>();
  readonly userName = input<string>('');
  /** Recensione già lasciata, per precompilare voto e testo quando si modifica. */
  readonly initialReview = input<Review | null>(null);

  readonly saved = output<Review>();
  readonly deleted = output<void>();
  readonly closed = output<void>();

  protected readonly stars = [1, 2, 3, 4, 5];
  protected readonly maxLength = MAX_TESTO_LENGTH;
  protected readonly saving = signal(false);
  protected readonly deleting = signal(false);
  protected readonly hoveredStar = signal(0);

  protected readonly form = new FormGroup({
    valutazione: new FormControl(0, { nonNullable: true, validators: [Validators.min(1), Validators.max(5)] }),
    testo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(MAX_TESTO_LENGTH)],
    }),
  });

  protected readonly isEditing = computed(() => !!this.initialReview());
  protected readonly length = signal(0);

  constructor() {
    this.form.controls.testo.valueChanges.subscribe((value) => this.length.set(value.length));

    // input() non è ancora legato nell'inizializzatore dei campi: si applica qui, alla creazione.
    effect(() => {
      const initial = this.initialReview();
      untracked(() => {
        if (initial) this.form.patchValue({ valutazione: initial.valutazione, testo: initial.testo });
      });
    });
  }

  protected readonly canSave = computed(() => {
    const { valutazione, testo } = this.form.getRawValue();
    return valutazione >= 1 && valutazione <= 5 && !!testo.trim() && testo.length <= MAX_TESTO_LENGTH && !this.saving();
  });

  @HostListener('document:keydown.escape')
  protected close(): void {
    this.closed.emit();
  }

  protected setStar(value: number): void {
    this.form.controls.valutazione.setValue(value);
  }

  protected save(): void {
    if (!this.canSave()) return;

    const { valutazione, testo } = this.form.getRawValue();
    this.saving.set(true);
    this.reviewService.upsert(this.userId(), { valutazione, testo: testo.trim() }).subscribe({
      next: (review) => {
        this.saving.set(false);
        this.toast.success(this.isEditing() ? 'Recensione aggiornata' : 'Recensione pubblicata');
        this.saved.emit(review);
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.toast.error(err.message);
      },
    });
  }

  protected delete(): void {
    if (this.deleting() || !confirm('Vuoi davvero eliminare questa recensione?')) return;

    this.deleting.set(true);
    this.reviewService.delete(this.userId()).subscribe({
      next: () => {
        this.deleting.set(false);
        this.toast.success('Recensione eliminata');
        this.deleted.emit();
      },
      error: (err: ApiError) => {
        this.deleting.set(false);
        this.toast.error(err.message);
      },
    });
  }
}
