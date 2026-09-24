import { Directive, ElementRef, OnDestroy, OnInit, inject, output } from '@angular/core';

/**
 * Emette {@link reached} quando l'elemento (una "sentinella" in fondo alla lista) sta per
 * entrare nello schermo: usato per caricare la pagina successiva del feed senza click.
 */
@Directive({ selector: '[appInfiniteScroll]' })
export class InfiniteScrollDirective implements OnInit, OnDestroy {
  private readonly el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  readonly reached = output<void>();

  ngOnInit(): void {
    if (typeof IntersectionObserver === 'undefined') return; // resta il pulsante "Carica altri"
    // rootMargin: inizia a caricare ~600px prima di arrivare in fondo
    this.observer = new IntersectionObserver((entries) => entries[0]?.isIntersecting && this.reached.emit(), {
      rootMargin: '600px 0px',
    });
    this.observer.observe(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
