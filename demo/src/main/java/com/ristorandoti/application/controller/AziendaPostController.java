package com.ristorandoti.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.CreatePostRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.PostDto;
import com.ristorandoti.application.entity.PostVisibilita;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione dei post di una pagina aziendale dalla Dashboard: lista completa (pubblici e privati),
 * nascondi/mostra, rimuovi. Tutte le rotte richiedono il JWT.
 *
 * <p>La pubblicazione di un nuovo post resta su {@link PostController} ({@code POST
 * /api/posts/azienda/{aziendaId}}, già gated da {@code MANAGE_POSTS}): qui vivono solo le
 * funzioni nuove di questa fase, annidate sotto {@code /api/aziende/{aziendaId}} secondo la
 * stessa convenzione degli altri endpoint della Dashboard.</p>
 */
@RestController
@RequestMapping("/api/aziende/{aziendaId}/posts")
@RequiredArgsConstructor
public class AziendaPostController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final PostService postService;

    /**
     * @param aziendaId azienda di cui leggere i post
     * @param stato     {@code PUBBLICO}/{@code PRIVATO} per filtrare, assente per tutti
     * @return {@code 200 OK} con una pagina dei post (pubblici e privati), dal più recente,
     *         {@code 403} se l'utente autenticato non ha {@code VIEW_DASHBOARD}
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<PostDto>> lista(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                           @RequestParam(required = false) PostVisibilita stato,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(
                postService.getPostsByAziendaDashboard(aziendaId, JwtService.extractUserId(jwt), stato, page, size));
    }

    /**
     * @param request nuovo testo e/o foto, già validati
     * @return {@code 200 OK} con il post aggiornato, {@code 403} se l'utente autenticato non ha
     *         {@code MANAGE_POSTS}, {@code 404} se il post non esiste o non appartiene a questa azienda
     */
    @PutMapping("/{postId}")
    public ResponseEntity<PostDto> modifica(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                            @PathVariable Long postId, @Valid @RequestBody CreatePostRequestDto request) {
        return ResponseEntity.ok(
                postService.updateAziendaPost(aziendaId, postId, JwtService.extractUserId(jwt), request));
    }

    /**
     * @return {@code 204 No Content}, {@code 403} se l'utente autenticato non ha {@code MANAGE_POSTS},
     *         {@code 404} se il post non esiste o non appartiene a questa azienda
     */
    @PostMapping("/{postId}/nascondi")
    public ResponseEntity<Void> nascondi(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId, @PathVariable Long postId) {
        postService.hide(aziendaId, postId, JwtService.extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    /**
     * @return {@code 204 No Content}, {@code 403} se l'utente autenticato non ha {@code MANAGE_POSTS},
     *         {@code 404} se il post non esiste o non appartiene a questa azienda
     */
    @PostMapping("/{postId}/mostra")
    public ResponseEntity<Void> mostra(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId, @PathVariable Long postId) {
        postService.unhide(aziendaId, postId, JwtService.extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    /**
     * Rimuove un post (soft-delete, richiede conferma lato client prima di chiamare questa rotta).
     *
     * @return {@code 204 No Content}, {@code 403} se l'utente autenticato non ha {@code MANAGE_POSTS},
     *         {@code 404} se il post non esiste o non appartiene a questa azienda
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> rimuovi(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId, @PathVariable Long postId) {
        postService.delete(aziendaId, postId, JwtService.extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }
}
