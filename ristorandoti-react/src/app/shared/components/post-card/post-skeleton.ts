import { Component } from '@angular/core';

/** Segnaposto animato mostrato mentre si caricano i post. */
@Component({
  selector: 'app-post-skeleton',
  template: `
    <div class="card animate-pulse p-5" aria-hidden="true">
      <div class="flex items-center gap-3">
        <div class="size-11 rounded-full bg-slate-200"></div>
        <div class="flex-1 space-y-2">
          <div class="h-3 w-1/3 rounded bg-slate-200"></div>
          <div class="h-2.5 w-1/2 rounded bg-slate-100"></div>
        </div>
      </div>
      <div class="mt-5 space-y-2.5">
        <div class="h-3 rounded bg-slate-100"></div>
        <div class="h-3 w-5/6 rounded bg-slate-100"></div>
        <div class="h-3 w-2/3 rounded bg-slate-100"></div>
      </div>
    </div>
  `,
  host: { class: 'block' },
})
export class PostSkeleton {}
