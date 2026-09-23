import { Injectable } from '@angular/core';

import { environment } from '../../../environments/environment';

const GSI_SRC = 'https://accounts.google.com/gsi/client';

/** Carica una sola volta lo script di Google Identity Services. */
@Injectable({ providedIn: 'root' })
export class GoogleIdentityService {
  readonly clientId = environment.googleClientId;
  readonly enabled = !!this.clientId;

  private loading?: Promise<void>;

  load(): Promise<void> {
    if (typeof google !== 'undefined' && google.accounts?.id) {
      return Promise.resolve();
    }

    this.loading ??= new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = GSI_SRC;
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => {
        this.loading = undefined;
        script.remove();
        reject(new Error('Impossibile caricare Google Identity Services'));
      };
      document.head.appendChild(script);
    });

    return this.loading;
  }
}
