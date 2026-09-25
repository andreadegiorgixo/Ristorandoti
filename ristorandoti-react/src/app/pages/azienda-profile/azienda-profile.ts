import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiError } from '../../core/http/api-error';
import { Azienda, AziendaPersona, FASCE_PREZZO, FasciaPrezzo, TIPI_AZIENDA, TipoAzienda } from '../../core/models/azienda.models';
import { MAX_OFFERTE_ATTIVE, OffertaLavoro } from '../../core/models/lavoro.models';
import { Post } from '../../core/models/post.models';
import { AziendaService } from '../../core/services/azienda.service';
import { CandidaturaService } from '../../core/services/candidatura.service';
import { FollowService } from '../../core/services/follow.service';
import { MetricsService } from '../../core/services/metrics.service';
import { OffertaLavoroService } from '../../core/services/offerta-lavoro.service';
import { PostService } from '../../core/services/post.service';
import { ToastService } from '../../core/services/toast.service';
import { PersonaCard } from '../../shared/components/persona-card/persona-card';
import { PostCard } from '../../shared/components/post-card/post-card';
import { ExpiresInPipe } from '../../shared/pipes/expires-in.pipe';
import { OverviewTextPipe } from '../../shared/pipes/overview-text.pipe';

type Tab = 'home' | 'lavoro';

/**
 * Vista pubblica di un'azienda (/azienda/:aziendaId): logo, banner, dati e servizi offerti
 * (tab HOME), più l'ultimo post e alcune persone che ci lavorano, e le offerte di lavoro attive
 * (tab LAVORO, con possibilità di candidarsi). Identica per tutti, admin compreso: chi ha
 * {@code puoiVedereDashboard} vede in più il pulsante "Visualizzala come dashboard" al posto di
 * "Modifica" (spostata nella Dashboard, sezione Impostazioni). La gestione delle offerte
 * (pubblica/modifica/chiudi) vive anch'essa solo nella Dashboard: qui si può solo vedere e candidarsi.
 */
@Component({
  selector: 'app-azienda-profile',
  imports: [RouterLink, PersonaCard, PostCard, ExpiresInPipe, OverviewTextPipe],
  templateUrl: './azienda-profile.html',
})
export class AziendaProfile {
  private readonly aziendaService = inject(AziendaService);
  private readonly followService = inject(FollowService);
  private readonly postService = inject(PostService);
  private readonly offertaLavoroService = inject(OffertaLavoroService);
  private readonly candidaturaService = inject(CandidaturaService);
  private readonly metricsService = inject(MetricsService);
  private readonly toast = inject(ToastService);
  private readonly title = inject(Title);
  private readonly route = inject(ActivatedRoute);

  readonly aziendaId = input.required<string>();

  protected readonly data = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly tab = signal<Tab>('home');
  protected readonly followPending = signal(false);

  protected readonly ultimoPost = signal<Post | null>(null);
  protected readonly personePreview = signal<AziendaPersona[]>([]);
  protected readonly personeTotal = signal(0);

  protected readonly offerte = signal<OffertaLavoro[]>([]);
  protected readonly offerteLoading = signal(true);
  protected readonly candidaturaPending = signal<number | null>(null);

  private readonly tipoLabelByValue = new Map<TipoAzienda, string>(TIPI_AZIENDA.map((t) => [t.value, t.label]));
  private readonly fasciaByValue = new Map<FasciaPrezzo, (typeof FASCE_PREZZO)[number]>(FASCE_PREZZO.map((f) => [f.value, f]));

  protected readonly fascia = computed(() => {
    const value = this.data()?.fasciaPrezzo;
    return value ? (this.fasciaByValue.get(value) ?? null) : null;
  });

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => {
        this.load(id);
        if (this.route.snapshot.fragment === 'lavoro') this.tab.set('lavoro');
      });
    });
    effect(() => {
      const nome = this.data()?.nome;
      if (nome) this.title.setTitle(`${nome} — Ristorandoti`);
    });
  }

  protected tipoLabel(tipo: TipoAzienda): string {
    return this.tipoLabelByValue.get(tipo) ?? tipo;
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()));
  }

  /**
   * Segue/smette di seguire la pagina aziendale. Ottimistico come il follow tra persone.
   */
  protected toggleFollow(): void {
    if (this.followPending()) return;

    const previous = this.data();
    if (!previous) return;

    const following = !previous.followedByMe;
    this.followPending.set(true);
    this.data.set({ ...previous, followedByMe: following, followersCount: previous.followersCount + (following ? 1 : -1) });

    const request = following ? this.followService.followAzienda(previous.id) : this.followService.unfollowAzienda(previous.id);
    request.subscribe({
      next: (status) => {
        this.followPending.set(false);
        this.data.update((a) => (a ? { ...a, ...status } : a));
      },
      error: (err: ApiError) => {
        this.followPending.set(false);
        this.data.set(previous);
        this.toast.error(err.message);
      },
    });
  }

  protected onPostChange(post: Post): void {
    this.ultimoPost.set(post);
  }

  /** Invia una candidatura per un'offerta di lavoro. Chiunque può candidarsi. */
  protected candidati(offerta: OffertaLavoro): void {
    if (this.candidaturaPending() === offerta.id || offerta.candidaturaGiaInviata) return;
    this.candidaturaPending.set(offerta.id);

    this.candidaturaService.candidati(offerta.aziendaId, offerta.id).subscribe({
      next: () => {
        this.candidaturaPending.set(null);
        this.offerte.update((list) => list.map((o) => (o.id === offerta.id ? { ...o, candidaturaGiaInviata: true } : o)));
        this.toast.success('Candidatura inviata!');
      },
      error: (err: ApiError) => {
        this.candidaturaPending.set(null);
        this.toast.error(err.message);
      },
    });
  }

  private load(id: number): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.data.set(null);
    this.tab.set('home');
    this.ultimoPost.set(null);
    this.personePreview.set([]);
    this.personeTotal.set(0);
    this.offerte.set([]);
    this.offerteLoading.set(true);

    this.aziendaService.getById(id).subscribe({
      next: (azienda) => {
        this.data.set(azienda);
        this.loading.set(false);
        // Solo la vista pubblica registra una visualizzazione: mai dalla Dashboard, altrimenti
        // chi gestisce la pagina gonfierebbe le proprie statistiche (il backend esclude comunque
        // chi ha accesso alla Dashboard, come ulteriore rete di sicurezza).
        this.metricsService.recordPageView(id);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });

    this.postService.getByAzienda(id, 0, 1).subscribe({
      next: (page) => this.ultimoPost.set(page.content[0] ?? null),
      error: () => this.ultimoPost.set(null),
    });

    this.aziendaService.getPersone(id, 0, 4).subscribe({
      next: (page) => {
        this.personePreview.set(page.content);
        this.personeTotal.set(page.totalElements);
      },
      error: () => this.personePreview.set([]),
    });

    this.offertaLavoroService.getByAzienda(id, 0, MAX_OFFERTE_ATTIVE).subscribe({
      next: (page) => {
        this.offerte.set(page.content);
        this.offerteLoading.set(false);
      },
      error: (err: ApiError) => {
        this.toast.error(err.message);
        this.offerteLoading.set(false);
      },
    });
  }
}
