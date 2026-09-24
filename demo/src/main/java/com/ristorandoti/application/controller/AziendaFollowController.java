package com.ristorandoti.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.FollowStatusDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.AziendaFollowService;

import lombok.RequiredArgsConstructor;

/**
 * Controller REST del "follow" delle pagine aziendali. Tutte le rotte richiedono il JWT.
 *
 * <p>L'utente che segue/smette di seguire è sempre ricavato dalla claim {@code uid} del token,
 * mai da un parametro della richiesta.</p>
 */
@RestController
@RequestMapping("/api/aziende/{aziendaId}/follow")
@RequiredArgsConstructor
public class AziendaFollowController {

    private final AziendaFollowService aziendaFollowService;

    /**
     * @param aziendaId azienda da iniziare a seguire
     * @return {@code 200 OK} con lo stato aggiornato, {@code 404} se l'azienda non esiste
     */
    @PostMapping
    public ResponseEntity<FollowStatusDto> follow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId) {
        return ResponseEntity.ok(aziendaFollowService.follow(JwtService.extractUserId(jwt), aziendaId));
    }

    /**
     * @param aziendaId azienda da smettere di seguire
     * @return {@code 200 OK} con lo stato aggiornato (idempotente)
     */
    @DeleteMapping
    public ResponseEntity<FollowStatusDto> unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId) {
        return ResponseEntity.ok(aziendaFollowService.unfollow(JwtService.extractUserId(jwt), aziendaId));
    }
}
