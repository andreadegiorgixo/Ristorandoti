import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Almeno una lettera e un numero. */
export const passwordStrength: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value as string;
  if (!value) return null;
  return /[A-Za-z]/.test(value) && /\d/.test(value) ? null : { weakPassword: true };
};

/** Validatore di gruppo: verifica che i due campi coincidano. */
export function matchFields(field: string, confirmField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const a = group.get(field)?.value;
    const b = group.get(confirmField)?.value;
    return a && b && a !== b ? { fieldsMismatch: true } : null;
  };
}
