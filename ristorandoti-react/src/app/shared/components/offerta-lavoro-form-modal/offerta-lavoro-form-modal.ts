import { Component, HostListener, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { OffertaLavoro } from '../../../core/models/lavoro.models';
import { OffertaLavoroService } from '../../../core/services/offerta-lavoro.service';

const LIMITS = { titolo: 200, descrizione: 2000 };

/**
 * Finestra modale per pubblicare una nuova offerta di lavoro per un'azienda. Chi la apre ha già
 * superato il controllo lato client (meno di 3 offerte attive, {@code gestibileDaMe}): il limite
 * resta comunque applicato anche lato backend, che risponde 400 se già raggiunto.
 */
@Component({
  selector: 'app-offerta-lavoro-form-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './offerta-lavoro-form-modal.html',
})
export class OffertaLavoroFormModal {
  private readonly offertaLavoroService = inject(OffertaLavoroService);

  readonly aziendaId = input.required<number>();

  readonly saved = output<OffertaLavoro>();
  readonly closed = output<void>();

  protected readonly limits = LIMITS;
  protected readonly saving = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly form = new FormGroup({
    titolo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(LIMITS.titolo)] }),
    descrizione: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.descrizione)] }),
  });

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
    this.offertaLavoroService.create(this.aziendaId(), { titolo: titolo.trim(), descrizione: descrizione.trim() || undefined }).subscribe({
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
