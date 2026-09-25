import { Component, HostListener, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { Language, LanguageLevel, Profile } from '../../../core/models/profile.models';
import { ProfileService } from '../../../core/services/profile.service';
import { LANGUAGE_LEVELS } from '../../utils/language-levels';
import { LANGUAGES } from '../../utils/languages';

const MAX_SUGGESTIONS = 8;

/**
 * Finestra modale per aggiungere una lingua conosciuta al profilo: nome cercabile da un elenco
 * chiuso e livello separato per scritto e parlato. Il salvataggio sostituisce l'intera lista
 * lingue del profilo (stesso pattern "replace all" di esperienze e formazione), quindi riceve
 * quelle già presenti per non perderle.
 */
@Component({
  selector: 'app-language-form-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './language-form-modal.html',
})
export class LanguageFormModal {
  private readonly profileService = inject(ProfileService);

  /** Lingue già salvate nel profilo, per il merge al salvataggio e per evitare duplicati. */
  readonly existing = input.required<Language[]>();

  readonly saved = output<Profile>();
  readonly closed = output<void>();

  protected readonly levels = LANGUAGE_LEVELS;
  protected readonly saving = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly suggestionsOpen = signal(false);

  protected readonly form = new FormGroup({
    lingua: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    livelloScritto: new FormControl<LanguageLevel | ''>('', { nonNullable: true, validators: [Validators.required] }),
    livelloParlato: new FormControl<LanguageLevel | ''>('', { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly alreadyAdded = computed(
    () => new Set(this.existing().map((l) => l.lingua.toLocaleLowerCase('it-IT'))),
  );

  protected readonly suggestions = computed(() => {
    const query = this.query().trim().toLocaleLowerCase('it-IT');
    const taken = this.alreadyAdded();
    return LANGUAGES.filter((l) => !taken.has(l.toLocaleLowerCase('it-IT')))
      .filter((l) => !query || l.toLocaleLowerCase('it-IT').includes(query))
      .slice(0, MAX_SUGGESTIONS);
  });

  private readonly query = signal('');

  /** Il form è reattivo (RxJS), non a segnali: serve un ponte per farlo leggere da `canSave`. */
  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly canSave = computed(() => {
    const { lingua, livelloScritto, livelloParlato } = this.formValue();
    const linguaNormalizzata = (lingua ?? '').trim().toLocaleLowerCase('it-IT');
    const isKnownLanguage = LANGUAGES.some((l) => l.toLocaleLowerCase('it-IT') === linguaNormalizzata);
    return isKnownLanguage && !this.alreadyAdded().has(linguaNormalizzata)
      && !!livelloScritto && !!livelloParlato && !this.saving();
  });

  @HostListener('document:keydown.escape')
  protected close(): void {
    if (this.saving()) return;
    this.closed.emit();
  }

  protected onLinguaInput(value: string): void {
    this.query.set(value);
    this.suggestionsOpen.set(true);
  }

  protected selectLanguage(lingua: string): void {
    this.form.controls.lingua.setValue(lingua);
    this.query.set(lingua);
    this.suggestionsOpen.set(false);
  }

  protected submit(): void {
    if (!this.canSave()) {
      this.form.markAllAsTouched();
      return;
    }

    const { lingua, livelloScritto, livelloParlato } = this.form.getRawValue();
    const nuova = { lingua, livelloScritto: livelloScritto as LanguageLevel, livelloParlato: livelloParlato as LanguageLevel };
    const payload = [
      ...this.existing().map((l) => ({ lingua: l.lingua, livelloScritto: l.livelloScritto, livelloParlato: l.livelloParlato })),
      nuova,
    ];

    this.saving.set(true);
    this.error.set(null);
    this.profileService.updateMe({ lingue: payload }).subscribe({
      next: (profile) => {
        this.saving.set(false);
        this.saved.emit(profile);
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.error.set(err);
      },
    });
  }
}
