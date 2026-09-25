import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';

import { ApiError } from '../../../core/http/api-error';
import { DashboardMetriche, METRICHE_DASHBOARD, MetricSerie, MetricaDashboard } from '../../../core/models/dashboard.models';
import { MetricsService } from '../../../core/services/metrics.service';
import { LineChart, LineChartSerie } from '../../../shared/components/line-chart/line-chart';
import { MetricCard } from '../../../shared/components/metric-card/metric-card';

type IntervalloGiorni = 7 | 30 | 90 | 365;

const INTERVALLI: ReadonlyArray<{ value: IntervalloGiorni; label: string }> = [
  { value: 7, label: '7 giorni' },
  { value: 30, label: '30 giorni' },
  { value: 90, label: '90 giorni' },
  { value: 365, label: '12 mesi' },
];

const dateFmt = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'short' });
const dateFmtLungo = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', year: 'numeric' });

/**
 * Home della Dashboard: le quattro metriche di rendimento della pagina (card con valore di
 * periodo e variazione % vs periodo precedente) e il grafico del loro andamento nel tempo.
 * Sola lettura per tutti i ruoli (compreso l'Admin): non ci sono azioni da compiere qui.
 */
@Component({
  selector: 'app-dashboard-home',
  imports: [MetricCard, LineChart],
  templateUrl: './dashboard-home.html',
})
export class DashboardHome {
  private readonly metricsService = inject(MetricsService);

  readonly aziendaId = input.required<string>();

  protected readonly intervalli = INTERVALLI;
  protected readonly metricheDisponibili = METRICHE_DASHBOARD;

  protected readonly intervallo = signal<IntervalloGiorni>(30);
  protected readonly metricheSelezionate = signal<Set<MetricaDashboard>>(new Set(METRICHE_DASHBOARD.map((m) => m.value)));

  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly dati = signal<DashboardMetriche | null>(null);

  protected readonly datiDisponibiliDal = computed(() => {
    const serie = this.dati()?.serie.find((s) => s.datiDisponibiliDal);
    return serie?.datiDisponibiliDal ? dateFmtLungo.format(new Date(serie.datiDisponibiliDal)) : null;
  });

  /** {@code false} solo se l'azienda non ha mai avuto un solo evento registrato: stato vuoto vero e proprio. */
  protected readonly haDati = computed(() => (this.dati()?.serie ?? []).some((s) => !!s.datiDisponibiliDal));

  protected readonly etichetteGiorni = computed(() => {
    const serie = this.dati()?.serie[0];
    return serie ? serie.punti.map((p) => dateFmt.format(new Date(p.giorno))) : [];
  });

  protected readonly serieGrafico = computed<LineChartSerie[]>(() => {
    const selezionate = this.metricheSelezionate();
    return (this.dati()?.serie ?? [])
      .filter((s) => selezionate.has(s.metrica))
      .map((s) => ({
        etichetta: this.etichettaMetrica(s.metrica),
        colore: this.coloreMetrica(s.metrica),
        valori: s.punti.map((p) => p.valore),
      }));
  });

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      const giorni = this.intervallo();
      untracked(() => this.load(id, giorni));
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()), this.intervallo());
  }

  protected serieDi(metrica: MetricaDashboard): MetricSerie | undefined {
    return this.dati()?.serie.find((s) => s.metrica === metrica);
  }

  protected toggleMetrica(metrica: MetricaDashboard): void {
    this.metricheSelezionate.update((attuali) => {
      const next = new Set(attuali);
      if (next.has(metrica)) {
        if (next.size > 1) next.delete(metrica); // sempre almeno una metrica selezionata nel grafico
      } else {
        next.add(metrica);
      }
      return next;
    });
  }

  protected etichettaMetrica(metrica: MetricaDashboard): string {
    return this.metricheDisponibili.find((m) => m.value === metrica)?.label ?? metrica;
  }

  protected coloreMetrica(metrica: MetricaDashboard): string {
    return this.metricheDisponibili.find((m) => m.value === metrica)?.colore ?? '#64748b';
  }

  private load(id: number, giorni: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);

    const al = new Date();
    const dal = new Date(al);
    dal.setDate(dal.getDate() - (giorni - 1));

    this.metricsService
      .getSeries(id, { dal: toIso(dal), al: toIso(al), metriche: this.metricheDisponibili.map((m) => m.value) })
      .subscribe({
        next: (dati) => {
          this.dati.set(dati);
          this.loading.set(false);
        },
        error: (err: ApiError) => {
          this.error.set(err);
          this.loading.set(false);
        },
      });
  }
}

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10);
}
