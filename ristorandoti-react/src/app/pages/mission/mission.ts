import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-mission',
  imports: [RouterLink],
  templateUrl: './mission.html',
})
export class Mission {
  protected readonly pillars = [
    {
      title: 'Condividere',
      text: 'Piatti, ricette, storie di cucina e di sala: ogni ristoratore ha qualcosa di prezioso da raccontare.',
    },
    {
      title: 'Farsi vedere',
      text: 'Un profilo che mostra chi sei e cosa fai, per farti notare da clienti, colleghi e professionisti del settore.',
    },
    {
      title: 'Collaborare',
      text: 'Eventi a quattro mani, fornitori, nuove figure per il team: le opportunità nascono dalle relazioni.',
    },
  ];

  protected readonly steps = [
    { title: 'Crea il tuo profilo', text: 'Presenta il tuo locale o il tuo ruolo in pochi minuti.' },
    { title: 'Pubblica e racconta', text: 'Condividi piatti, attività e momenti del tuo lavoro.' },
    { title: 'Connettiti', text: 'Scopri altri ristoratori e avvia nuove collaborazioni.' },
  ];
}
