import { Component, computed, input, linkedSignal } from '@angular/core';

type AvatarSize = 'sm' | 'md' | 'lg' | 'xl';

const SIZES: Record<AvatarSize, string> = {
  sm: 'size-9 text-xs',
  md: 'size-11 text-sm',
  lg: 'size-16 text-lg',
  xl: 'size-28 text-3xl sm:size-36 sm:text-4xl',
};

/** Foto profilo circolare; se manca o non si carica mostra le iniziali del nome. */
@Component({
  selector: 'app-avatar',
  template: `
    @if (src() && !broken()) {
      <img
        [src]="src()"
        [alt]="'Foto di ' + name()"
        (error)="broken.set(true)"
        loading="lazy"
        class="shrink-0 rounded-full bg-slate-100 object-cover {{ sizeClass() }} {{ ring() ? 'ring-4 ring-white' : '' }}"
      />
    } @else {
      <span
        class="grid shrink-0 place-items-center rounded-full bg-brand-600 font-bold text-white select-none {{ sizeClass() }} {{ ring() ? 'ring-4 ring-white' : '' }}"
        [attr.aria-label]="name()"
        role="img"
      >{{ initials() }}</span>
    }
  `,
  host: { class: 'contents' },
})
export class Avatar {
  readonly src = input<string | null | undefined>(null);
  readonly name = input.required<string>();
  readonly size = input<AvatarSize>('md');
  /** Bordo bianco, per l'avatar sovrapposto al banner. */
  readonly ring = input(false);

  /** Torna false quando cambia l'immagine, così un nuovo URL viene ritentato. */
  protected readonly broken = linkedSignal({ source: this.src, computation: () => false });
  protected readonly sizeClass = computed(() => SIZES[this.size()]);
  protected readonly initials = computed(() =>
    this.name()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]!.toUpperCase())
      .join(''),
  );
}
