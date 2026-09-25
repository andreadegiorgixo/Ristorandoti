import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../http/api-error';
import { DashboardMetriche, MetricaDashboard } from '../models/dashboard.models';

export interface RangeMetriche {
  /** Data ISO (yyyy-MM-dd), inclusa */
  dal: string;
  /** Data ISO (yyyy-MM-dd), inclusa */
  al: string;
  metriche: MetricaDashboard[];
}

/** Metriche della Home Dashboard: lettura del grafico e tracciamento delle visualizzazioni. */
@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);

  getSeries(aziendaId: number, range: RangeMetriche): Observable<DashboardMetriche> {
    let params = new HttpParams().set('dal', range.dal).set('al', range.al);
    range.metriche.forEach((metrica) => {
      params = params.append('metriche', metrica);
    });
    return this.http
      .get<DashboardMetriche>(`${environment.apiUrl}/aziende/${aziendaId}/dashboard/metriche`, { params })
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }

  /**
   * Registra una visualizzazione della pagina pubblica. Va chiamato solo dalla vista pubblica,
   * mai dalla Dashboard. Fire-and-forget: un errore qui (rete, rate limit) non deve mai interrompere
   * la navigazione dell'utente, quindi non propaga né mostra un toast.
   */
  recordPageView(aziendaId: number): void {
    this.http.post<void>(`${environment.apiUrl}/aziende/${aziendaId}/metriche/eventi/visualizzazione`, {}).subscribe({
      error: () => void 0,
    });
  }
}
