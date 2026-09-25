import { LanguageLevel } from '../../core/models/profile.models';

/** Livelli di conoscenza di una lingua (LanguageLevel), da base a madrelingua. */
export const LANGUAGE_LEVELS: readonly { value: LanguageLevel; label: string }[] = [
  { value: 'BASE', label: 'Base' },
  { value: 'INTERMEDIO', label: 'Intermedio' },
  { value: 'CONOSCENZA_PROFESSIONALE', label: 'Conoscenza professionale' },
  { value: 'MADRELINGUA', label: 'Madrelingua' },
];

const LABELS: Record<LanguageLevel, string> = Object.fromEntries(
  LANGUAGE_LEVELS.map((l) => [l.value, l.label]),
) as Record<LanguageLevel, string>;

export function languageLevelLabel(level: LanguageLevel): string {
  return LABELS[level];
}
