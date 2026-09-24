package com.ristorandoti.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.ReviewDto;
import com.ristorandoti.application.dto.ReviewEligibilityDto;
import com.ristorandoti.application.dto.ReviewRequestDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST delle recensioni tra colleghi. Tutte le rotte richiedono il JWT.
 *
 * <p>L'autore di una recensione è sempre ricavato dalla claim {@code uid} del token, mai da un
 * parametro della richiesta: così nessuno può scrivere una recensione a nome di un altro utente.
 * Il requisito "stesso locale, periodo sovrapposto" è verificato server-side in
 * {@link ReviewService#upsert}, non solo per abilitare il pulsante lato client.</p>
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final ReviewService reviewService;

    /**
     * @param userId utente di cui leggere le recensioni ricevute
     * @return {@code 200 OK} con una pagina di recensioni, {@code 404} se l'utente non esiste
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponseDto<ReviewDto>> getReviews(@PathVariable Long userId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId, page, size));
    }

    /**
     * @param userId utente che si vorrebbe recensire
     * @return {@code 200 OK} con {@code canReview}/{@code alreadyReviewed}, {@code 404} se non esiste
     */
    @GetMapping("/eligibility/{userId}")
    public ResponseEntity<ReviewEligibilityDto> getEligibility(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getEligibility(JwtService.extractUserId(jwt), userId));
    }

    /**
     * Crea o aggiorna la recensione dell'utente autenticato verso {@code userId}.
     *
     * @param userId  utente da recensire
     * @param request voto e testo
     * @return {@code 200 OK} con la recensione salvata, {@code 400} se non si può recensirlo,
     *         {@code 404} se non esiste
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ReviewDto> upsert(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId,
                                            @Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.ok(reviewService.upsert(JwtService.extractUserId(jwt), userId, request));
    }

    /**
     * Elimina la recensione dell'utente autenticato verso {@code userId} (idempotente).
     *
     * @param userId destinatario della recensione da eliminare
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        reviewService.delete(JwtService.extractUserId(jwt), userId);
        return ResponseEntity.noContent().build();
    }
}
