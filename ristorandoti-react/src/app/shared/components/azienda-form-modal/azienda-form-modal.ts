import { Component, HostListener, inject, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { Azienda, FASCE_PREZZO, FasciaPrezzo, TIPI_AZIENDA, TipoAzienda } from '../../../core/models/azienda.models';
import { AziendaService } from '../../../core/services/azienda.service';
import { ToastService } from '../../../core/services/toast.service';
import { IMAGE_ACCEPT, UploadService } from '../../../core/services/upload.service';

/** Limiti allineati ad AziendaRequestDto. */
const LIMITS = { nome: 200, descrizione: 2000, indirizzo: 300, citta: 100, telefono: 30, email: 255, sitoWebUrl: 1000, servizio: 100 };
const MAX_SERVIZI = 15;

type ImageField = 'fotoProfiloUrl' | 'bannerUrl';

/** Finestra modale con il form per creare un'azienda: dati, logo, banner, fascia di prezzo e servizi. */
@Component({
  selector: 'app-azienda-form-modal',
  imports: [ReactiveFormsModule, FormsModule],
  templateUrl: './azienda-form-modal.html',
})
export class AziendaFormModal {
  private readonly aziendaService = inject(AziendaService);
  private readonly uploadService = inject(UploadService);
  private readonly toast = inject(ToastService);

  readonly created = output<Azienda>();
  readonly closed = output<void>();

  protected readonly tipi = TIPI_AZIENDA;
  protected readonly fasce = FASCE_PREZZO;
  protected readonly limits = LIMITS;
  protected readonly maxServizi = MAX_SERVIZI;
  protected readonly imageAccept = IMAGE_ACCEPT;
  protected readonly saving = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  /** Immagine in caricamento (logo o banner). */
  protected readonly uploading = signal<ImageField | null>(null);

  protected readonly servizi = signal<string[]>([]);
  protected servizioInput = '';

  protected readonly form = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(LIMITS.nome)] }),
    tipo: new FormControl<TipoAzienda | null>(null, { validators: [Validators.required] }),
    citta: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.citta)] }),
    indirizzo: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.indirizzo)] }),
    telefono: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.telefono)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email, Validators.maxLength(LIMITS.email)] }),
    sitoWebUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.sitoWebUrl)] }),
    descrizione: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.descrizione)] }),
    fotoProfiloUrl: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    bannerUrl: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    fasciaPrezzo: new FormControl<FasciaPrezzo | null>(null, { validators: [Validators.required] }),
  });

  protected readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  @HostListener('document:keydown.escape')
  protected close(): void {
    if (this.saving() || this.uploading()) return;
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

  protected onImageSelected(field: ImageField, input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = ''; // permette di riscegliere lo stesso file
    if (!file) return;

    this.uploading.set(field);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.uploading.set(null);
        const control = this.form.controls[field];
        control.setValue(url);
        control.setErrors(null);
        control.markAsDirty();
      },
      error: (err: ApiError) => {
        this.uploading.set(null);
        this.toast.error(err.message);
      },
    });
  }

  protected clearImage(field: ImageField): void {
    this.form.controls[field].setValue('');
  }

  protected addServizio(): void {
    const value = this.servizioInput.trim();
    this.servizioInput = '';
    if (!value || value.length > LIMITS.servizio) return;
    if (this.servizi().length >= MAX_SERVIZI) return;
    if (this.servizi().includes(value)) return;
    this.servizi.update((list) => [...list, value]);
  }

  protected removeServizio(index: number): void {
    this.servizi.update((list) => list.filter((_, i) => i !== index));
  }

  protected submit(): void {
    if (this.uploading()) return;
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
        fotoProfiloUrl: v.fotoProfiloUrl,
        bannerUrl: v.bannerUrl,
        fasciaPrezzo: v.fasciaPrezzo!,
        servizi: this.servizi(),
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
