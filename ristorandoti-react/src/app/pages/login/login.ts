import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { afterLoginUrl } from '../../core/guards/auth.guard';
import { AuthError } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { GoogleSignInButton } from '../../shared/components/google-sign-in-button/google-sign-in-button';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, GoogleSignInButton],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = new FormGroup({
    // Allineato a LoginRequestDto: l'email è lo username di login
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected readonly loading = signal(false);
  protected readonly error = signal<AuthError | null>(null);
  protected readonly showPassword = signal(false);

  /** Da passare alla registrazione, così anche dopo l'iscrizione si torna alla pagina richiesta. */
  protected readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

  protected hasError(control: 'email' | 'password'): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth
      .login({ ...this.form.getRawValue(), email: this.form.controls.email.value.trim() })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.router.navigateByUrl(afterLoginUrl(this.returnUrl)),
        error: (err: AuthError) => this.error.set(err),
      });
  }
}
