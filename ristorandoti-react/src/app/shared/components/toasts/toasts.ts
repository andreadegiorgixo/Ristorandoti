import { Component, inject } from '@angular/core';

import { ToastService } from '../../../core/services/toast.service';

/** Contenitore delle notifiche, in basso al centro (sopra la barra di salvataggio). */
@Component({
  selector: 'app-toasts',
  template: `
    <div class="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex flex-col items-center gap-2 px-4 sm:bottom-6" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <div
          class="pointer-events-auto flex w-full max-w-sm items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium shadow-lg ring-1"
          [class]="
            t.kind === 'success'
              ? 'bg-brand-900 text-white ring-brand-800'
              : t.kind === 'error'
                ? 'bg-red-600 text-white ring-red-700'
                : 'bg-slate-900 text-white ring-slate-800'
          "
          role="status"
        >
          @switch (t.kind) {
            @case ('success') {
              <svg class="size-5 shrink-0 text-brand-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5" /></svg>
            }
            @case ('error') {
              <svg class="size-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 8v4M12 16h.01" /></svg>
            }
            @default {
              <svg class="size-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 16v-4M12 8h.01" /></svg>
            }
          }
          <span class="flex-1">{{ t.message }}</span>
          <button type="button" (click)="toast.dismiss(t.id)" class="-mr-1 rounded p-1 opacity-70 hover:opacity-100" aria-label="Chiudi notifica">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12" /></svg>
          </button>
        </div>
      }
    </div>
  `,
})
export class Toasts {
  protected readonly toast = inject(ToastService);
}
