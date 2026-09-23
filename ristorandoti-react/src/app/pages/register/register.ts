import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthError } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { GoogleSignInButton } from '../../shared/components/google-sign-in-button/google-sign-in-button';
import { matchFields, passwordStrength } from '../../core/validators/password.validators';

type Field = 'firstName' | 'lastName' | 'email' | 'password' | 'confirmPassword';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, GoogleSignInButton],
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Limiti allineati a RegisterRequestDto: name ≤ 150, email ≤ 255, password 8–72
  protected readonly form = new FormGroup(
    {
      firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(70)] }),
      lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(70)] }),
      email: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.email, Validators.maxLength(255)],
      }),
      password: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72), passwordStrength],
      }),
      confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: matchFields('password', 'confirmPassword') },
  );

  protected readonly loading = signal(false);
  protected readonly error = signal<AuthError | null>(null);
  protected readonly showPassword = signal(false);

  protected hasError(field: Field): boolean {
    const c = this.form.controls[field];
    const touched = c.touched || c.dirty;
    if (field === 'confirmPassword') {
      return touched && (c.invalid || this.form.hasError('fieldsMismatch'));
    }
    return touched && c.invalid;
  }

  protected errorMessage(field: Field): string {
    const e = this.form.controls[field].errors ?? {};

    if (e['server']) return e['server'];
    if (e['required']) return 'Campo obbligatorio.';
    if (e['email']) return 'Inserisci un indirizzo email valido.';
    if (e['minlength']) return `Minimo ${e['minlength'].requiredLength} caratteri.`;
    if (e['maxlength']) return `Massimo ${e['maxlength'].requiredLength} caratteri.`;
    if (e['weakPassword']) return 'Deve contenere almeno una lettera e un numero.';
    if (field === 'confirmPassword' && this.form.hasError('fieldsMismatch')) return 'Le password non coincidono.';
    return '';
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const { firstName, lastName, email, password } = this.form.getRawValue();

    this.auth
      .register({ name: `${firstName.trim()} ${lastName.trim()}`, email: email.trim(), password })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        // Il backend restituisce già il JWT: l'utente è autenticato
        next: () => this.router.navigateByUrl('/'),
        error: (err: AuthError) => {
          this.error.set(err);
          this.applyServerErrors(err);
        },
      });
  }

  /** Mostra sotto ai singoli campi i fieldErrors del GlobalExceptionHandler. */
  private applyServerErrors(err: AuthError): void {
    const controls: Record<string, AbstractControl> = {
      name: this.form.controls.firstName,
      email: this.form.controls.email,
      password: this.form.controls.password,
    };

    for (const [field, message] of Object.entries(err.fieldErrors ?? {})) {
      const control = controls[field];
      if (control) {
        control.setErrors({ ...control.errors, server: message });
        control.markAsTouched();
      }
    }
  }
}
