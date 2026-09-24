import { Component, HostListener, inject, output, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { Azienda, TIPI_AZIENDA, TipoAzienda } from '../../../core/models/azienda.models';
import { AziendaService } from '../../../core/services/azienda.service';

/** Limiti allineati ad AziendaRequestDto. */
const LIMITS = { nome: 200, descrizione: 2000, indirizzo: 300, citta: 100, telefono: 30, email: 255, sitoWebUrl: 1000 };

/** Finestra modale con il form per creare un'azienda. */
@Component({
  selector: 'app-azienda-form-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './azienda-form-modal.html',
})
export class AziendaFormModal {
  private readonly aziendaService = inject(AziendaService);

  readonly created = output<Azienda>();
  readonly closed = output<void>();

  protected readonly tipi = TIPI_AZIENDA;
  protected readonly limits = LIMITS;
  protected readonly saving = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly form = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(LIMITS.nome)] }),
    tipo: new FormControl<TipoAzienda | null>(null, { validators: [Validators.required] }),
    citta: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.citta)] }),
    indirizzo: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.indirizzo)] }),
    telefono: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.telefono)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email, Validators.maxLength(LIMITS.email)] }),
    sitoWebUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.sitoWebUrl)] }),
    descrizione: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.descrizione)] }),
  });

  @HostListener('document:keydown.escape')
  protected close(): void {
    if (this.saving()) return;
    this.closed.emit();
  }

  protected showError(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  protected errorText(control: AbstractControl | null): string {
    const e = control?.errors ?? {};
    if (e['server']) return e['server'];
    if (e['required']) return 'Campo obbligatorio.';
    if (e['email']) return 'Inserisci un indirizzo email valido.';
    if (e['maxlength']) return `Massimo ${e['maxlength'].requiredLength} caratteri.`;
    return '';
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const v = this.form.getRawValue();
    this.aziendaService
      .create({
        nome: v.nome.trim(),
        tipo: v.tipo!,
        citta: v.citta.trim() || undefined,
        indirizzo: v.indirizzo.trim() || undefined,
        telefono: v.telefono.trim() || undefined,
        email: v.email.trim() || undefined,
        sitoWebUrl: v.sitoWebUrl.trim() || undefined,
        descrizione: v.descrizione.trim() || undefined,
      })
      .subscribe({
        next: (azienda) => {
          this.saving.set(false);
          this.created.emit(azienda);
        },
        error: (err: ApiError) => {
          this.saving.set(false);
          this.error.set(err);
          this.applyServerErrors(err.fieldErrors);
        },
      });
  }

  private applyServerErrors(fieldErrors?: Record<string, string>): void {
    for (const [field, message] of Object.entries(fieldErrors ?? {})) {
      const control = this.form.get(field);
      if (!control) continue;
      control.setErrors({ ...control.errors, server: message });
      control.markAsTouched();
    }
  }
}
