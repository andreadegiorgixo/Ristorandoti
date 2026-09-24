import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiError, toApiError } from '../http/api-error';

/** Formati e dimensione accettati da POST /api/uploads/images (spring.servlet.multipart.max-file-size). */
export const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp';
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

@Injectable({ providedIn: 'root' })
export class UploadService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/uploads`;

  /** Carica un'immagine dal dispositivo e restituisce il suo URL pubblico. */
  uploadImage(file: File): Observable<string> {
    const invalid = checkImage(file);
    if (invalid) return throwError(() => invalid);

    const body = new FormData();
    body.append('file', file);
    return this.http.post<{ url: string }>(`${this.baseUrl}/images`, body).pipe(
      map((res) => res.url),
      catchError((err) => throwError(() => toApiError(err))),
    );
  }
}

/** Controllo anticipato lato client, per non caricare 10 MB solo per sentirsi dire di no. */
function checkImage(file: File): ApiError | null {
  if (!IMAGE_ACCEPT.split(',').includes(file.type)) {
    return { status: 400, message: 'Formato non supportato: scegli un’immagine JPG, PNG, GIF o WebP.' };
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return { status: 413, message: 'L’immagine è troppo grande: il limite è 10 MB.' };
  }
  return null;
}
