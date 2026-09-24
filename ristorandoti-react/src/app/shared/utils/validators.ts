import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validatore di gruppo: la data di fine (se presente) non può precedere quella di inizio.
 * Lavora su valori "AAAA-MM" (confronto lessicografico = cronologico). Imposta l'errore
 * `endBeforeStart` sul gruppo.
 */
export function periodValidator(startKey = 'dataStart', endKey = 'dataEnd'): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const start = group.get(startKey)?.value as string;
    const end = group.get(endKey)?.value as string;
    return start && end && end < start ? { endBeforeStart: true } : null;
  };
}

/** La data ("AAAA-MM") non può essere nel futuro. */
export function notInFuture(max: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    return value && value > max ? { future: true } : null;
  };
}
