import { CanDeactivateFn } from '@angular/router';

/** Componenti con un form che può avere modifiche non salvate. */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

/** Chiede conferma prima di lasciare una pagina con modifiche non salvate. */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) =>
  !component.hasUnsavedChanges() || confirm('Hai modifiche non salvate. Vuoi davvero uscire?');
