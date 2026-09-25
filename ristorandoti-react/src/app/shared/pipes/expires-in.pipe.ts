import { Pipe, PipeTransform } from '@angular/core';

/** "3g 4h rimanenti", "2h rimanenti", "Scaduta": tempo restante prima di una scadenza ISO-8601. */
@Pipe({ name: 'expiresIn' })
export class ExpiresInPipe implements PipeTransform {
  transform(iso: string): string {
    const msRimanenti = new Date(iso).getTime() - Date.now();
    if (msRimanenti <= 0) return 'Scaduta';
    const oreTotali = Math.floor(msRimanenti / 3_600_000);
    const giorni = Math.floor(oreTotali / 24);
    return giorni > 0 ? `${giorni}g ${oreTotali % 24}h rimanenti` : `${oreTotali}h rimanenti`;
  }
}
