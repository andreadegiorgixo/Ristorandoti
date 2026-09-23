import { Component } from '@angular/core';

@Component({
  selector: 'app-about',
  templateUrl: './about.html',
})
export class About {
  protected readonly portfolioUrl = 'https://www.andreadegiorgio.io';

  // TODO: personalizza le tappe del tuo percorso
  protected readonly milestones = [
    { title: 'Sviluppo software', text: 'Progetto e realizzo applicazioni web moderne, dal frontend alle API.' },
    { title: 'Passione per la ristorazione', text: 'Conosco da vicino i ritmi e le sfide di chi lavora in sala e in cucina.' },
    { title: 'Ristorandoti', text: 'Unisco le due cose in una piattaforma che mette in contatto i ristoratori.' },
  ];
}
