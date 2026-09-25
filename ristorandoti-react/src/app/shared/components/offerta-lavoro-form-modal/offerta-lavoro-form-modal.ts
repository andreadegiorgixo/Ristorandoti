import { Component, HostListener, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { OffertaLavoro } from '../../../core/models/lavoro.models';
import { OffertaLavoroService } from '../../../core/services/offerta-lavoro.service';

const LIMITS = { titolo: 200, descrizione: 2000 };

/**
 * Finestra modale per pubblicare una nuova offerta di lavoro, o modificarne una esistente
 * (valorizzando {@link offerta}, stesso pattern di {@code AziendaFormModal}). Chi apre la
 * creazione ha già superato il controllo lato client (meno di 3 offerte attive): il limite resta
 * comunque applicato anche lato backend, che risponde 400 se già raggiunto; una modifica su
 * un'offerta scaduta nel frattempo riceve invece un messaggio di conflitto dal backend.
 */
@Component({
  selector: 'app-offerta-lavoro-form-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './offerta-lavoro-form-modal.html',
})
export class OffertaLavoroFormModal {
  private readonly offertaLavoroService = inject(OffertaLavoroService);

  readonly aziendaId = input.required<number>();
  /** Offerta da modificare; assente in creazione. */
  readonly offerta = input<OffertaLavoro | null>(null);
  protected readonly isEdit = computed(() => !!this.offerta());

  readonly saved = output<OffertaLavoro>();
  readonly closed = output<void>();

  protected readonly limits = LIMITS;
  protected readonly saving = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly form = new FormGroup({
    titolo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(LIMITS.titolo)] }),
    descrizione: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.descrizione)] }),
  });

  constructor() {
    effect(() => {
      const offerta = this.offerta();
      if (offerta) {
        this.form.setValue({ titolo: offerta.titolo, descrizione: offerta.descrizione ?? '' });
      }
    });
  }

  @HostListener('document:keydown.escape')
  protected close(): void {
    if (this.saving()) return;
    this.closed.emit();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const { titolo, descrizione } = this.form.getRawValue();
    const payload = { titolo: titolo.trim(), descrizione: descrizione.trim() || undefined };
    const offertaEsistente = this.offerta();
    const request = offertaEsistente
      ? this.offertaLavoroService.update(this.aziendaId(), offertaEsistente.id, payload)
      : this.offertaLavoroService.create(this.aziendaId(), payload);

    request.subscribe({
      next: (offerta) => {
        this.saving.set(false);
        this.saved.emit(offerta);
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.error.set(err);
      },
    });
  }
}
