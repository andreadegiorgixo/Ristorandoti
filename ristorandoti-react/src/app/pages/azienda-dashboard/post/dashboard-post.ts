import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiError } from '../../../core/http/api-error';
import { Azienda } from '../../../core/models/azienda.models';
import { Post, PostVisibilita } from '../../../core/models/post.models';
import { AziendaService } from '../../../core/services/azienda.service';
import { DashboardPermissionService } from '../../../core/services/dashboard-permission.service';
import { PostService } from '../../../core/services/post.service';
import { ToastService } from '../../../core/services/toast.service';
import { PostComposer } from '../../../shared/components/post-composer/post-composer';
import { RelativeTimePipe } from '../../../shared/pipes/relative-time.pipe';

type Filtro = 'TUTTI' | PostVisibilita;

const FILTRI: ReadonlyArray<{ value: Filtro; label: string }> = [
  { value: 'TUTTI', label: 'Tutti' },
  { value: 'PUBBLICO', label: 'Pubblici' },
  { value: 'PRIVATO', label: 'Privati' },
];

const MAX_LUNGHEZZA_MODIFICA = 3000;

/**
 * Gestione post della pagina: pubblicazione (riusa {@link PostComposer}), modifica del testo,
 * nascondi/mostra (cambia visibilità senza cancellare) e rimozione (soft-delete, con conferma).
 * Sola lettura per chi non ha {@code MANAGE_POSTS} (comunque visibile, come da matrice di default).
 */
@Component({
  selector: 'app-dashboard-post',
  imports: [FormsModule, PostComposer, RelativeTimePipe],
  templateUrl: './dashboard-post.html',
})
export class DashboardPost {
  private readonly aziendaService = inject(AziendaService);
  private readonly postService = inject(PostService);
  private readonly toast = inject(ToastService);
  protected readonly permissionService = inject(DashboardPermissionService);

  readonly aziendaId = input.required<string>();

  protected readonly azienda = signal<Azienda | null>(null);
  protected readonly filtri = FILTRI;
  protected readonly filtro = signal<Filtro>('TUTTI');
  protected readonly maxLunghezza = MAX_LUNGHEZZA_MODIFICA;

  protected readonly posts = signal<Post[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly editingId = signal<number | null>(null);
  protected readonly editText = signal('');
  protected readonly saving = signal(false);

  protected readonly puoiScrivere = computed(() => this.permissionService.can('MANAGE_POSTS'));

  constructor() {
    effect(() => {
      const id = Number(this.aziendaId());
      untracked(() => this.aziendaService.getById(id).subscribe({ next: (a) => this.azienda.set(a) }));
    });
    effect(() => {
      const id = Number(this.aziendaId());
      const filtro = this.filtro();
      untracked(() => this.load(id, filtro));
    });
  }

  protected retry(): void {
    this.load(Number(this.aziendaId()), this.filtro());
  }

  protected onPublished(post: Post): void {
    this.posts.update((list) => [post, ...list]);
  }

  protected startEdit(post: Post): void {
    this.editingId.set(post.id);
    this.editText.set(post.contenuto ?? '');
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected saveEdit(post: Post): void {
    const testo = this.editText().trim();
    if (!testo && !post.mediaUrl) return; // un post non può restare del tutto vuoto

    this.saving.set(true);
    this.postService.updateAzienda(Number(this.aziendaId()), post.id, { contenuto: testo || undefined, mediaUrl: post.mediaUrl ?? undefined }).subscribe({
      next: (aggiornato) => {
        this.saving.set(false);
        this.posts.update((list) => list.map((p) => (p.id === post.id ? aggiornato : p)));
        this.editingId.set(null);
        this.toast.success('Post modificato.');
      },
      error: (err: ApiError) => {
        this.saving.set(false);
        this.toast.error(err.message);
      },
    });
  }

  protected toggleVisibilita(post: Post): void {
    const aziendaId = Number(this.aziendaId());
    const request = post.visibilita === 'PUBBLICO' ? this.postService.nascondi(aziendaId, post.id) : this.postService.mostra(aziendaId, post.id);
    const nuovaVisibilita: PostVisibilita = post.visibilita === 'PUBBLICO' ? 'PRIVATO' : 'PUBBLICO';

    request.subscribe({
      next: () => {
        this.aggiornaVisibilitaLocale(post.id, nuovaVisibilita);
        this.toast.success(nuovaVisibilita === 'PRIVATO' ? 'Post nascosto.' : 'Post reso di nuovo pubblico.');
      },
      error: (err: ApiError) => this.toast.error(err.message),
    });
  }

  protected rimuovi(post: Post): void {
    if (!confirm('Vuoi rimuovere questo post? Non sarà più visibile né gestibile, ma i like già ricevuti restano nel totale storico.')) {
      return;
    }
    this.postService.rimuovi(Number(this.aziendaId()), post.id).subscribe({
      next: () => {
        this.posts.update((list) => list.filter((p) => p.id !== post.id));
        this.toast.success('Post rimosso.');
      },
      error: (err: ApiError) => this.toast.error(err.message),
    });
  }

  private aggiornaVisibilitaLocale(postId: number, visibilita: PostVisibilita): void {
    if (this.filtro() !== 'TUTTI' && this.filtro() !== visibilita) {
      this.posts.update((list) => list.filter((p) => p.id !== postId));
      return;
    }
    this.posts.update((list) => list.map((p) => (p.id === postId ? { ...p, visibilita } : p)));
  }

  private load(id: number, filtro: Filtro): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);

    this.postService.getByAziendaDashboard(id, 0, 50, filtro === 'TUTTI' ? undefined : filtro).subscribe({
      next: (pagina) => {
        this.posts.set(pagina.content);
        this.loading.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }
}
