import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, map, of, switchMap } from 'rxjs';

import { HasUnsavedChanges } from '../../core/guards/unsaved-changes.guard';
import { ApiError } from '../../core/http/api-error';
import { Azienda } from '../../core/models/azienda.models';
import { Education, Experience, Profile, ProfileUpdateRequest } from '../../core/models/profile.models';
import { AziendaService } from '../../core/services/azienda.service';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { ToastService } from '../../core/services/toast.service';
import { IMAGE_ACCEPT, UploadService } from '../../core/services/upload.service';
import { Avatar } from '../../shared/components/avatar/avatar';
import { currentMonth, formatPeriod, isoToMonth, monthToIso } from '../../shared/utils/dates';
import { notInFuture, periodValidator } from '../../shared/utils/validators';

/** Limiti allineati ai DTO del backend. */
const LIMITS = { sommario: 500, azienda: 200, ruolo: 150, descrizione: 2000, istituto: 200, titolo: 200 };

/** Sotto questa lunghezza non si cerca ancora (evita chiamate inutili). */
const AZIENDA_SEARCH_MIN_LENGTH = 2;

type ImageField = 'profilePictureUrl' | 'bannerUrl';

type ExperienceForm = FormGroup<{
  azienda: FormControl<string>;
  aziendaId: FormControl<number | null>;
  ruolo: FormControl<string>;
  dataStart: FormControl<string>;
  current: FormControl<boolean>;
  dataEnd: FormControl<string>;
  descrizione: FormControl<string>;
}>;

type EducationForm = FormGroup<{
  istituto: FormControl<string>;
  titoloStudio: FormControl<string>;
  dataStart: FormControl<string>;
  current: FormControl<boolean>;
  dataEnd: FormControl<string>;
}>;

/**
 * Modifica del proprio profilo in un'unica pagina: immagini con anteprima, sommario,
 * esperienze e formazione (aggiungi / modifica / rimuovi). Un solo "Salva" invia tutto.
 */
@Component({
  selector: 'app-profile-edit',
  imports: [ReactiveFormsModule, RouterLink, Avatar],
  templateUrl: './profile-edit.html',
})
export class ProfileEdit implements OnInit, HasUnsavedChanges {
  private readonly profileService = inject(ProfileService);
  private readonly aziendaService = inject(AziendaService);
  private readonly uploadService = inject(UploadService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly auth = inject(AuthService);

  protected readonly limits = LIMITS;
  protected readonly imageAccept = IMAGE_ACCEPT;
  protected readonly maxMonth = currentMonth();
  protected readonly formatPeriod = formatPeriod;

  protected readonly loading = signal(true);
  /** Il form è stato popolato almeno una volta (dalla cache o dal server). */
  protected readonly ready = signal(false);
  protected readonly loadError = signal<ApiError | null>(null);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  /** Immagine in caricamento (foto profilo o banner). */
  protected readonly uploading = signal<ImageField | null>(null);

  /** Voci di esperienza/formazione aperte in modifica (le altre mostrano il riepilogo). */
  protected readonly openItems = signal(new Set<AbstractControl>());

  /** Ricerca aziende per il collegamento dell'esperienza (tipo-mentre-scrivi). */
  private readonly aziendaQuery$ = new Subject<{ group: ExperienceForm; term: string }>();
  protected readonly aziendaSearchGroup = signal<ExperienceForm | null>(null);
  protected readonly aziendaSuggestions = signal<Azienda[]>([]);
  protected readonly aziendaSearching = signal(false);

  private readonly aziendaQuerySub = this.aziendaQuery$
    .pipe(
      debounceTime(300),
      switchMap(({ group, term }) => {
        const trimmed = term.trim();
        if (trimmed.length < AZIENDA_SEARCH_MIN_LENGTH) {
          this.aziendaSearching.set(false);
          return of({ group, results: [] as Azienda[] });
        }
        this.aziendaSearching.set(true);
        return this.aziendaService.search(trimmed).pipe(
          map((result) => ({ group, results: result.content })),
          catchError(() => of({ group, results: [] as Azienda[] })),
        );
      }),
      takeUntilDestroyed(),
    )
    .subscribe(({ group, results }) => {
      this.aziendaSearching.set(false);
      if (this.aziendaSearchGroup() === group) this.aziendaSuggestions.set(results);
    });

  protected readonly form = new FormGroup({
    // URL restituiti da POST /api/uploads/images dopo aver scelto il file
    profilePictureUrl: new FormControl('', { nonNullable: true }),
    bannerUrl: new FormControl('', { nonNullable: true }),
    sommario: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LIMITS.sommario)] }),
    esperienze: new FormArray<ExperienceForm>([]),
    istruzione: new FormArray<EducationForm>([]),
  });

  protected readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly pictureUrl = computed(() => this.value().profilePictureUrl || null);
  protected readonly bannerUrl = computed(() => this.value().bannerUrl || null);
  protected readonly sommarioLength = computed(() => this.value().sommario?.length ?? 0);

  /** Sommario proposto in un clic a partire dall'esperienza attuale, es. "Sous Chef presso Da Mario". */
  protected readonly suggestedSommario = computed(() => {
    const current = this.value().esperienze?.find((e) => e.current && e.ruolo?.trim() && e.azienda?.trim());
    const suggestion = current ? `${current.ruolo!.trim()} presso ${current.azienda!.trim()}` : null;
    return suggestion && suggestion !== this.value().sommario?.trim() ? suggestion : null;
  });

  get esperienze(): FormArray<ExperienceForm> {
    return this.form.controls.esperienze;
  }

  get istruzione(): FormArray<EducationForm> {
    return this.form.controls.istruzione;
  }

  ngOnInit(): void {
    const cached = this.profileService.me();
    if (cached) this.populate(cached);

    // Si riparte sempre dai dati più recenti del server
    this.profileService.getMe().subscribe({
      next: (profile) => {
        if (!this.form.dirty) this.populate(profile);
        this.loading.set(false);
        this.scrollToFragment();
      },
      error: (err: ApiError) => {
        this.loadError.set(err);
        this.loading.set(false);
      },
    });
  }

  hasUnsavedChanges(): boolean {
    return (this.form.dirty || !!this.uploading()) && !this.saving();
  }

  // ---------- Esperienze / formazione ----------

  /** @param markDirty false quando la voce è aperta automaticamente (non è ancora una modifica dell'utente) */
  protected addExperience(markDirty = true): void {
    const group = this.experienceGroup();
    this.esperienze.insert(0, group);
    if (markDirty) this.form.markAsDirty();
    this.open(group, 'exp-0-ruolo');
  }

  protected addEducation(markDirty = true): void {
    const group = this.educationGroup();
    this.istruzione.insert(0, group);
    if (markDirty) this.form.markAsDirty();
    this.open(group, 'edu-0-istituto');
  }

  protected removeAt(array: FormArray, index: number): void {
    array.removeAt(index);
    this.form.markAsDirty();
  }

  protected isOpen(control: AbstractControl): boolean {
    return this.openItems().has(control);
  }

  protected toggle(control: AbstractControl): void {
    // Una voce non valida resta aperta finché non viene corretta
    if (this.isOpen(control) && control.invalid) {
      control.markAllAsTouched();
      return;
    }
    this.openItems.update((set) => {
      const next = new Set(set);
      if (next.has(control)) {
        next.delete(control);
      } else {
        next.add(control);
      }
      return next;
    });
  }

  /** L'utente digita nel campo azienda: cerca aziende registrate corrispondenti. */
  protected onAziendaInput(group: ExperienceForm, term: string): void {
    if (group.controls.aziendaId.value !== null) group.controls.aziendaId.setValue(null);
    this.aziendaSearchGroup.set(group);
    this.aziendaQuery$.next({ group, term });
  }

  /** L'utente sceglie un'azienda dai suggerimenti: si collega e si allinea il nome mostrato. */
  protected selectAzienda(group: ExperienceForm, azienda: Azienda): void {
    group.patchValue({ azienda: azienda.nome, aziendaId: azienda.id });
    this.form.markAsDirty();
    this.closeAziendaSuggestions();
  }

  /** Toglie il collegamento, mantenendo il testo libero già inserito. */
  protected unlinkAzienda(group: ExperienceForm): void {
    group.controls.aziendaId.setValue(null);
    this.form.markAsDirty();
  }

  protected closeAziendaSuggestions(): void {
    this.aziendaSearchGroup.set(null);
    this.aziendaSuggestions.set([]);
  }

  /** "Lavoro qui attualmente" / "In corso": la data di fine non serve. */
  protected onCurrentChange(group: ExperienceForm | EducationForm): void {
    const end = group.controls.dataEnd;
    if (group.controls.current.value) {
      end.setValue('');
      end.disable();
    } else {
      end.enable();
    }
  }

  protected useSuggestion(text: string): void {
    this.form.controls.sommario.setValue(text);
    this.form.markAsDirty();
  }

  protected onImageSelected(field: ImageField, input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = ''; // permette di riscegliere lo stesso file
    if (!file) return;

    this.uploading.set(field);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.uploading.set(null);
        this.setImage(field, url);
      },
      error: (err: ApiError) => {
        this.uploading.set(null);
        this.toast.error(err.message);
      },
    });
  }

  protected clearImage(field: ImageField): void {
    this.setImage(field, '');
  }

  private setImage(field: ImageField, url: string): void {
    const control = this.form.controls[field];
    control.setValue(url);
    control.setErrors(null);
    this.form.markAsDirty();
  }

  protected showError(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  protected errorText(control: AbstractControl | null): string {
    const e = control?.errors ?? {};
    if (e['server']) return e['server'];
    if (e['required']) return 'Campo obbligatorio.';
    if (e['maxlength']) return `Massimo ${e['maxlength'].requiredLength} caratteri.`;
    if (e['future']) return 'La data non può essere nel futuro.';
    return '';
  }

  /** Errore di periodo del gruppo, mostrato sotto la data di fine. */
  protected periodError(group: AbstractControl): boolean {
    return group.hasError('endBeforeStart') && !!group.get('dataEnd')?.touched;
  }

  protected summary(group: ExperienceForm | EducationForm, openLabel: string): string {
    const { dataStart, dataEnd } = group.getRawValue();
    return dataStart ? formatPeriod(monthToIso(dataStart), dataEnd ? monthToIso(dataEnd) : null, openLabel) : '';
  }

  // ---------- Salvataggio ----------

  protected save(): void {
    this.saveError.set(null);

    if (this.uploading()) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.openInvalidItems();
      this.saveError.set('Controlla i campi evidenziati in rosso.');
      setTimeout(() => document.querySelector('[aria-invalid="true"]')?.scrollIntoView({ block: 'center', behavior: 'smooth' }));
      return;
    }

    this.saving.set(true);
    this.profileService.updateMe(this.toRequest()).subscribe({
      next: () => {
        this.saving.set(false);
        this.form.markAsPristine();
        this.toast.success('Profilo aggiornato!');
        this.router.navigateByUrl('/profile');
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.saveError.set(err.message);
        this.applyServerErrors(err.fieldErrors);
      },
    });
  }

  private toRequest(): ProfileUpdateRequest {
    const v = this.form.getRawValue();
    // Stringhe vuote incluse: per il backend "" significa "svuota il campo"
    return {
      profilePictureUrl: v.profilePictureUrl,
      bannerUrl: v.bannerUrl,
      sommario: v.sommario.trim(),
      esperienze: v.esperienze.map((e) => ({
        azienda: e.azienda.trim(),
        aziendaId: e.aziendaId,
        ruolo: e.ruolo.trim(),
        dataStart: monthToIso(e.dataStart),
        dataEnd: !e.current && e.dataEnd ? monthToIso(e.dataEnd) : null,
        descrizione: e.descrizione.trim() || null,
      })),
      istruzione: v.istruzione.map((e) => ({
        istituto: e.istituto.trim(),
        titoloStudio: e.titoloStudio.trim(),
        dataStart: monthToIso(e.dataStart),
        dataEnd: !e.current && e.dataEnd ? monthToIso(e.dataEnd) : null,
      })),
    };
  }

  /** Collega i fieldErrors del backend (es. "esperienze[0].azienda") ai campi del form. */
  private applyServerErrors(fieldErrors?: Record<string, string>): void {
    for (const [path, message] of Object.entries(fieldErrors ?? {})) {
      const match = /^(esperienze|istruzione)\[(\d+)]\.(\w+)$/.exec(path);
      let control: AbstractControl | null = null;

      if (match) {
        const item: AbstractControl | undefined = this.form.controls[match[1] as 'esperienze' | 'istruzione'].at(
          Number(match[2]),
        );
        const field = match[3] === 'periodoValido' ? 'dataEnd' : match[3];
        control = item?.get(field) ?? null;
        if (item) this.openItems.update((set) => new Set(set).add(item));
      } else {
        control = this.form.get(path);
      }

      control?.setErrors({ ...control.errors, server: message });
      control?.markAsTouched();
    }
  }

  // ---------- Supporto ----------

  private populate(profile: Profile): void {
    this.form.patchValue({
      profilePictureUrl: profile.profilePictureUrl ?? '',
      bannerUrl: profile.bannerUrl ?? '',
      sommario: profile.sommario ?? '',
    });
    this.esperienze.clear();
    profile.esperienze.forEach((e) => this.esperienze.push(this.experienceGroup(e)));
    this.istruzione.clear();
    profile.istruzione.forEach((e) => this.istruzione.push(this.educationGroup(e)));
    this.openItems.set(new Set());
    this.form.markAsPristine();
    this.ready.set(true);
  }

  private experienceGroup(e?: Experience): ExperienceForm {
    const group: ExperienceForm = new FormGroup(
      {
        azienda: new FormControl(e?.azienda ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(LIMITS.azienda)],
        }),
        aziendaId: new FormControl(e?.aziendaId ?? null),
        ruolo: new FormControl(e?.ruolo ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(LIMITS.ruolo)],
        }),
        dataStart: new FormControl(isoToMonth(e?.dataStart ?? null), {
          nonNullable: true,
          validators: [Validators.required, notInFuture(this.maxMonth)],
        }),
        current: new FormControl(!!e && !e.dataEnd, { nonNullable: true }),
        dataEnd: new FormControl(isoToMonth(e?.dataEnd ?? null), {
          nonNullable: true,
          validators: [notInFuture(this.maxMonth)],
        }),
        descrizione: new FormControl(e?.descrizione ?? '', {
          nonNullable: true,
          validators: [Validators.maxLength(LIMITS.descrizione)],
        }),
      },
      { validators: periodValidator() },
    );
    if (group.controls.current.value) group.controls.dataEnd.disable();
    return group;
  }

  private educationGroup(e?: Education): EducationForm {
    const group: EducationForm = new FormGroup(
      {
        istituto: new FormControl(e?.istituto ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(LIMITS.istituto)],
        }),
        titoloStudio: new FormControl(e?.titoloStudio ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(LIMITS.titolo)],
        }),
        dataStart: new FormControl(isoToMonth(e?.dataStart ?? null), {
          nonNullable: true,
          validators: [Validators.required, notInFuture(this.maxMonth)],
        }),
        current: new FormControl(!!e && !e.dataEnd, { nonNullable: true }),
        dataEnd: new FormControl(isoToMonth(e?.dataEnd ?? null), {
          nonNullable: true,
          validators: [notInFuture(this.maxMonth)],
        }),
      },
      { validators: periodValidator() },
    );
    if (group.controls.current.value) group.controls.dataEnd.disable();
    return group;
  }

  private open(control: AbstractControl, focusId: string): void {
    this.openItems.update((set) => new Set(set).add(control));
    setTimeout(() => document.getElementById(focusId)?.focus());
  }

  private openInvalidItems(): void {
    const invalid = [...this.esperienze.controls, ...this.istruzione.controls].filter((c) => c.invalid);
    this.openItems.update((set) => new Set([...set, ...invalid]));
  }

  /** Arrivando da "Aggiungi esperienza" ecc. (/profile/edit#esperienze) si scorre alla sezione. */
  private scrollToFragment(): void {
    const fragment = this.route.snapshot.fragment;
    if (!fragment) return;
    setTimeout(() => {
      document.getElementById(fragment)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      // Sezione vuota: si apre subito una nuova voce da compilare
      if (fragment === 'esperienze' && !this.esperienze.length) this.addExperience(false);
      if (fragment === 'istruzione' && !this.istruzione.length) this.addEducation(false);
    });
  }
}
