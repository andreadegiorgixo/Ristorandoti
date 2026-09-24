import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AziendaPersona } from '../../../core/models/azienda.models';
import { Avatar } from '../avatar/avatar';

/** Card "persona" (foto, nome, ruolo) usata nelle liste di chi lavora in un'azienda. */
@Component({
  selector: 'app-persona-card',
  imports: [RouterLink, Avatar],
  templateUrl: './persona-card.html',
  host: { class: 'block' },
})
export class PersonaCard {
  readonly persona = input.required<AziendaPersona>();
}
