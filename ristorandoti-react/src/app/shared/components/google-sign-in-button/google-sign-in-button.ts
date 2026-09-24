import { Component, DestroyRef, ElementRef, afterNextRender, inject, input, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';

import { afterLoginUrl } from '../../../core/guards/auth.guard';
import { AuthError } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleIdentityService } from '../../../core/services/google-identity.service';

/**
 * Pulsante ufficiale "Accedi con Google": riceve l'ID token da Google Identity Services
 * e lo scambia con il JWT applicativo tramite POST /api/auth/google.
 */
@Component({
  selector: 'app-google-sign-in-button',
  templateUrl: './google-sign-in-button.html',
})
export class GoogleSignInButton {
  private readonly auth = inject(AuthService);
  private readonly gis = inject(GoogleIdentityService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /** Testo del pulsante: "Accedi con Google" o "Registrati con Google" */
  readonly text = input<'signin_with' | 'signup_with'>('signin_with');
  readonly failed = output<AuthError>();

  protected readonly enabled = this.gis.enabled;
  protected readonly loading = signal(false);
  protected readonly scriptError = signal(false);

  private readonly container = viewChild<ElementRef<HTMLElement>>('container');

  constructor() {
    afterNextRender(() => {
      if (this.enabled) this.render();
    });
  }

  private async render(): Promise<void> {
    try {
      await this.gis.load();
    } catch {
      this.scriptError.set(true);
      return;
    }

    const el = this.container()?.nativeElement;
    if (!el) return;

    google.accounts.id.initialize({
      client_id: this.gis.clientId,
      callback: (res) => this.onCredential(res.credential),
      ux_mode: 'popup',
      cancel_on_tap_outside: true,
    });

    google.accounts.id.renderButton(el, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'rectangular',
      logo_alignment: 'center',
      text: this.text(),
      locale: 'it',
      // Google accetta larghezze tra 200 e 400 px
      width: Math.min(400, Math.max(200, el.offsetWidth)),
    });
  }

  private onCredential(idToken: string): void {
    this.loading.set(true);
    this.auth
      .loginWithGoogle(idToken)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.router.navigateByUrl(afterLoginUrl(this.route.snapshot.queryParamMap.get('returnUrl'))),
        error: (err: AuthError) => this.failed.emit(err),
      });
  }
}
