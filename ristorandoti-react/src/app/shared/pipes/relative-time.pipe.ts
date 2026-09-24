import { Pipe, PipeTransform } from '@angular/core';

const rtf = new Intl.RelativeTimeFormat('it-IT', { numeric: 'auto' });
const dateFmt = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'short' });
const dateFmtWithYear = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'short', year: 'numeric' });

/** "adesso", "5 minuti fa", "ieri", "3 giorni fa"; oltre la settimana la data ("12 set"). */
@Pipe({ name: 'relativeTime' })
export class RelativeTimePipe implements PipeTransform {
  transform(iso: string): string {
    const date = new Date(iso);
    const seconds = Math.round((date.getTime() - Date.now()) / 1000);
    const abs = Math.abs(seconds);

    if (abs < 45) return 'adesso';
    if (abs < 3600) return rtf.format(Math.round(seconds / 60), 'minute');
    if (abs < 86400) return rtf.format(Math.round(seconds / 3600), 'hour');
    if (abs < 7 * 86400) return rtf.format(Math.round(seconds / 86400), 'day');

    return date.getFullYear() === new Date().getFullYear() ? dateFmt.format(date) : dateFmtWithYear.format(date);
  }
}
