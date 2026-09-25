import { AfterViewInit, Component, ElementRef, OnDestroy, effect, input, viewChild } from '@angular/core';
import {
  CategoryScale,
  Chart,
  Filler,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js';

// Registrazione esplicita (non 'chart.js/auto'): solo i pezzi usati da un grafico a linee
// tengono il bundle più piccolo, coerente con la scelta di non introdurre una libreria pesante.
Chart.register(CategoryScale, LinearScale, LineController, LineElement, PointElement, Tooltip, Legend, Filler);

export interface LineChartSerie {
  etichetta: string;
  colore: string;
  valori: number[];
}

/**
 * Grafico a linee multi-serie (Chart.js). Il canvas non è ispezionabile da uno screen reader, per
 * questo il componente rende in parallelo una tabella `sr-only` con esattamente gli stessi dati:
 * è l'alternativa testuale richiesta per l'accessibilità, non un'aggiunta opzionale.
 */
@Component({
  selector: 'app-line-chart',
  templateUrl: './line-chart.html',
})
export class LineChart implements AfterViewInit, OnDestroy {
  readonly etichette = input.required<string[]>();
  readonly serie = input.required<LineChartSerie[]>();
  readonly ariaLabel = input('Andamento nel tempo');

  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const etichette = this.etichette();
      const serie = this.serie();
      if (this.chart) {
        this.chart.data.labels = etichette;
        this.chart.data.datasets = serie.map((s) => this.toDataset(s));
        this.chart.options.plugins!.legend!.display = serie.length > 1;
        this.chart.update();
      }
    });
  }

  ngAfterViewInit(): void {
    const ctx = this.canvasRef().nativeElement.getContext('2d');
    if (!ctx) return;

    this.chart = new Chart(ctx, {
      type: 'line',
      data: { labels: this.etichette(), datasets: this.serie().map((s) => this.toDataset(s)) },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { display: this.serie().length > 1, position: 'bottom' },
        },
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } },
        },
      },
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private toDataset(s: LineChartSerie) {
    return {
      label: s.etichetta,
      data: s.valori,
      borderColor: s.colore,
      backgroundColor: `${s.colore}33`,
      tension: 0.3,
      fill: false,
      pointRadius: 2,
    };
  }
}
