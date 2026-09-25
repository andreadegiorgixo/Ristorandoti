import { DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';

/** Card di una metrica della Home Dashboard: valore del periodo + variazione % vs periodo precedente. */
@Component({
  selector: 'app-metric-card',
  imports: [DecimalPipe],
  templateUrl: './metric-card.html',
})
export class MetricCard {
  readonly label = input.required<string>();
  readonly value = input<number | null>(null);
  readonly variazionePercento = input<number | null>(null);
  readonly loading = input(false);
}
