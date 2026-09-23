import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface Feature {
  title: string;
  description: string;
  icon: string;
}

interface PreviewPost {
  initials: string;
  author: string;
  place: string;
  tag: string;
  text: string;
  tone: string;
}

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
})
export class Home {
  protected readonly features: Feature[] = [
    {
      title: 'Condividi i tuoi piatti',
      description: 'Racconta le tue creazioni, le ricette di stagione e il lavoro che c’è dietro ogni servizio.',
      icon: 'M12 3a9 9 0 1 0 9 9M12 3v9l6-6',
    },
    {
      title: 'Racconta la tua storia',
      description: 'Il tuo locale, il tuo team, le sfide superate: dai voce al percorso che ti ha portato fin qui.',
      icon: 'M4 5h16v11H8l-4 4V5Z',
    },
    {
      title: 'Crea nuove opportunità',
      description: 'Entra in contatto con colleghi, fornitori e professionisti per collaborazioni e crescita.',
      icon: 'M16 11a4 4 0 1 0-8 0M3 21a7 7 0 0 1 18 0M19 8v6M22 11h-6',
    },
  ];

  // Contenuti dimostrativi per l'anteprima della community
  protected readonly previewPosts: PreviewPost[] = [
    {
      initials: 'TL',
      author: 'Trattoria da Lucia',
      place: 'Bologna',
      tag: 'Nuovo piatto',
      text: 'Tortellini in brodo di cappone: la ricetta della nonna torna in carta per l’autunno.',
      tone: 'bg-amber-100 text-amber-800',
    },
    {
      initials: 'MR',
      author: 'Marco R. · Chef',
      place: 'Napoli',
      tag: 'Collaborazione',
      text: 'Cerco un piccolo produttore di farine locali per il nuovo menù di pizze in degustazione.',
      tone: 'bg-sky-100 text-sky-800',
    },
  ];
}
