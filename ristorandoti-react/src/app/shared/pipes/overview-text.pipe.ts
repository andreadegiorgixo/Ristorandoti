import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Converte il testo semplice della Panoramica in HTML sicuro: fa sempre l'escape del testo prima
 * di applicare le uniche trasformazioni ammesse (paragrafi separati da righe vuote, righe che
 * iniziano con "- " come elenco puntato, URL nudi come link). Non usa mai {@code innerHTML} su
 * input utente grezzo: anche incollando `<script>` o un tag HTML, l'escape lo rende testo
 * letterale prima che qualunque trasformazione lo tocchi, quindi non serve una libreria di
 * sanitizzazione esterna.
 */
@Pipe({ name: 'overviewText' })
export class OverviewTextPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(testo: string | null | undefined): SafeHtml {
    if (!testo) return '';
    const html = testo
      .split(/\n{2,}/)
      .map((blocco) => this.renderBlocco(blocco))
      .join('');
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private renderBlocco(blocco: string): string {
    const righe = blocco.split('\n').map((riga) => riga.trim()).filter(Boolean);
    if (righe.length > 0 && righe.every((riga) => riga.startsWith('- '))) {
      const voci = righe.map((riga) => `<li>${this.linkify(this.escape(riga.slice(2)))}</li>`).join('');
      return `<ul>${voci}</ul>`;
    }
    return `<p>${this.linkify(this.escape(blocco)).replace(/\n/g, '<br>')}</p>`;
  }

  /** Sempre il primo passo: rende innocuo qualunque markup incollato dall'utente. */
  private escape(testo: string): string {
    return testo
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  /** Opera solo su testo già sottoposto a escape, quindi non può reintrodurre markup pericoloso. */
  private linkify(testoEscapato: string): string {
    return testoEscapato.replace(
      /(https?:\/\/[^\s<]+)/g,
      (url) => `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`,
    );
  }
}
