/**
 * Utility per le date di esperienze e formazione. Il backend usa date ISO complete
 * ("2022-01-01"); l'interfaccia lavora a mese/anno, come i curriculum.
 */

const monthFormatter = new Intl.DateTimeFormat('it-IT', { month: 'short', year: 'numeric' });

/** "2022-01-01" → "2022-01" (valore di un <input type="month">) */
export function isoToMonth(iso: string | null): string {
  return iso ? iso.slice(0, 7) : '';
}

/** "2022-01" → "2022-01-01" */
export function monthToIso(month: string): string {
  return `${month}-01`;
}

/** Mese corrente come "AAAA-MM", limite massimo dei campi data. */
export function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

/** "2022-01-01" → "gen 2022" */
export function formatMonth(iso: string): string {
  const [y, m] = iso.split('-').map(Number);
  return monthFormatter.format(new Date(y, m - 1, 1));
}

/**
 * Periodo leggibile con durata, es. "gen 2022 – Presente · 2 anni 3 mesi".
 *
 * @param openLabel etichetta per la data di fine assente ("Presente", "In corso")
 */
export function formatPeriod(start: string, end: string | null, openLabel = 'Presente'): string {
  const range = `${formatMonth(start)} – ${end ? formatMonth(end) : openLabel}`;
  const duration = formatDuration(start, end);
  return duration ? `${range} · ${duration}` : range;
}

function formatDuration(start: string, end: string | null): string {
  const [sy, sm] = start.split('-').map(Number);
  const endDate = end ? end.split('-').map(Number) : [new Date().getFullYear(), new Date().getMonth() + 1];
  // Si conta anche il mese iniziale: gen–mar = 3 mesi
  const months = (endDate[0] - sy) * 12 + (endDate[1] - sm) + 1;
  if (months <= 0) return '';

  const years = Math.floor(months / 12);
  const rest = months % 12;
  const parts: string[] = [];
  if (years) parts.push(years === 1 ? '1 anno' : `${years} anni`);
  if (rest) parts.push(rest === 1 ? '1 mese' : `${rest} mesi`);
  return parts.join(' ');
}
