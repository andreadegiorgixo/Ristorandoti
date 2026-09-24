import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <section class="mx-auto flex max-w-lg flex-col items-center px-4 py-24 text-center">
      <p class="text-sm font-bold text-brand-600">404</p>
      <h1 class="mt-3 text-3xl font-extrabold tracking-tight">Pagina non trovata</h1>
      <p class="mt-3 text-slate-600">La pagina che cerchi non esiste o è stata spostata.</p>
      <a routerLink="/" class="btn-primary mt-8">Torna all’inizio</a>
    </section>
  `,
})
export class NotFound {}
