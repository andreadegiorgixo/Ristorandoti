/** Visibilità di un post di pagina aziendale (i post personali sono sempre PUBBLICO). */
export type PostVisibilita = 'PUBBLICO' | 'PRIVATO';

/** Post del feed (PostDto) */
export interface Post {
  id: number;
  autoreId: number;
  autoreName: string;
  autoreProfilePictureUrl: string | null;
  autoreSommario: string | null;
  contenuto: string | null;
  mediaUrl: string | null;
  /** Istante ISO-8601 UTC */
  dataCreazione: string;
  likeCount: number;
  likedByMe: boolean;
  visibilita: PostVisibilita;
}

/** Body di POST /api/posts (CreatePostRequestDto): serve almeno testo o foto */
export interface CreatePostRequest {
  contenuto?: string;
  mediaUrl?: string;
}

/** Pagina di risultati (PageResponseDto) */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
