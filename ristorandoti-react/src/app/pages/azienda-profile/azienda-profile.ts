import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/http/api-error';
import { Azienda, AziendaPersona, FASCE_PREZZO, FasciaPrezzo, TIPI_AZIENDA, TipoAzienda } from '../../core/models/azienda.models';
import { MAX_OFFERTE_ATTIVE, OffertaLavoro } from '../../core/models/lavoro.models';
import { Post } from '../../core/models/post.models';
import { AuthService } from '../../core/services/auth.service';
import { AziendaService } from '../../core/services/azienda.service';
import { FollowService } from '../../core/services/follow.service';
import { OffertaLavoroService } from '../../core/services/offerta-lavoro.service';
import { PostService } from '../../core/services/post.service';
import { ToastService } from '../../core/services/toast.service';
import { AziendaFormModal } from '../../shared/components/azienda-form-modal/azienda-form-modal';
import { OffertaLavoroFormModal } from '../../shared/components/offerta-lavoro-form-modal/offerta-lavoro-form-modal';
import { PersonaCard } from '../../shared/components/persona-card/persona-card';
import { PostCard } from '../../shared/components/post-card/post-card';

type Tab = 'home' | 'lavoro';

/**
 * Profilo pubblico di un'azienda (/azienda/:aziendaId): logo, banner, dati e servizi offerti
 * (tab HOME), più l'ultimo post e alcune persone che ci lavorano, e le offerte di lavoro attive
 * (tab LAVORO). Solo proprietario e persone autorizzate ({@code gestibileDaMe}) vedono i
 * pulsanti "Modifica" e "Nuova offerta".
 */
@Component({
  selector: 'app-azienda-profile',
  imports: [RouterLink, AziendaFormModal, OffertaLavoroFormModal, PersonaCard, PostCard],
  templateUrl: './azienda-profile.html',
})
export class AziendaProfile {
  private readonly auth = inject(AuthService);
  private readonly aziendaService = inject(AziendaService);
  private readonly followService = inject(FollowService);
  private readonly postService = inject(PostService);
  private readonly offertaLavoroService = inject(OffertaLavoroService);
  private readonly toast = inject(ToastService);
  private readonly title = inject(Title);

  readonly aziendaId = input.required<string>();

  protected readonly data = signal<Azienda | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly editOpen = signal(false);
  protected readonly tab = signal<Tab>('home');
  protected readonly followPending = signal(false);

  protected readonly ultimoPost = signal<Post | null>(null);
  protected readonly personePreview = signal<AziendaPersona[]>([]);
  protected readonly personeTotal = signal(0);

  protected readonly offerte = signal<OffertaLavoro[]>([]);
  protected readonly offerteLoading = signal(true);
  protected readonly offertaFormOpen = signal(false);

  protected readonly isOwner = computed(() => {
    const azienda = this.data();
    const userId = this.auth.currentUser()?.id;
    return !!azienda && !!userId && azienda.proprietarioId === userId;
  });

  protected readonly puoiCreareOfferta = computed(() => (this.data()?.gestibileDaMe ?? false) && this.offerte().length < MAX_OFFERTE_ATTIVE);

  private readonly tipoLabelByValue = new Map<TipoAzienda, string>(TIPI_AZIENDA.map((t) => [t.value, t.label]));
  private readonly fasciaByValue = new Map<FasciaPrezzo, (typeof FASCE_PREZZO)[number]>(FASCE_PREZZO.map((f) => [f.value, f]));

  protected readonly fascia = computed(() => {
    const value = this.data()?.fasciaPrezzo;
    return value ? (this.fasciaByValue.get(value) ?? null) : null;
  });

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.load(id));
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

  protected onSaved(azienda: Azienda): void {
    this.data.set({ ...this.data(), ...azienda });
    this.editOpen.set(false);
    this.toast.success('Modifiche salvate.');
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

  protected openOffertaForm(): void {
    if (!this.puoiCreareOfferta()) return;
    this.offertaFormOpen.set(true);
  }

  protected onOffertaSaved(offerta: OffertaLavoro): void {
    this.offerte.update((list) => [offerta, ...list]);
    this.offertaFormOpen.set(false);
    this.toast.success('Offerta di lavoro pubblicata.');
  }

  protected chiudiOfferta(offerta: OffertaLavoro): void {
    if (!confirm(`Vuoi davvero chiudere l'offerta "${offerta.titolo}"?`)) return;

    this.offertaLavoroService.chiudi(offerta.aziendaId, offerta.id).subscribe({
      next: () => this.offerte.update((list) => list.filter((o) => o.id !== offerta.id)),
      error: (err: ApiError) => this.toast.error(err.message),
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
