package com.ristorandoti.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.ProfileDto;
import com.ristorandoti.application.dto.ProfileUpdateRequestDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST del profilo professionale. Tutte le rotte richiedono il JWT
 * (non sono in {@code PUBLIC_ENDPOINTS} di
 * {@link com.ristorandoti.application.config.SecurityConfig}).
 *
 * <p>L'utente "me" è ricavato dalla claim {@code uid} del token, mai da un parametro della
 * richiesta: così nessuno può modificare il profilo di un altro utente.</p>
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * @param jwt token dell'utente autenticato, iniettato da Spring Security
     * @return {@code 200 OK} con il profilo completo dell'utente loggato
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtService.extractUserId(jwt);
        return ResponseEntity.ok(profileService.getProfile(userId, userId));
    }

    /**
     * @param jwt    token dell'utente autenticato (per {@code followedByMe})
     * @param userId id dell'utente di cui visualizzare il profilo
     * @return {@code 200 OK} con il profilo, {@code 404} se l'utente non esiste
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getProfile(userId, JwtService.extractUserId(jwt)));
    }

    /**
     * Aggiorna il profilo dell'utente loggato (regole di merge in {@link ProfileUpdateRequestDto}).
     *
     * @param jwt     token dell'utente autenticato
     * @param request campi da modificare
     * @return {@code 200 OK} con il profilo aggiornato
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileDto> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody ProfileUpdateRequestDto request) {
        return ResponseEntity.ok(profileService.updateMyProfile(JwtService.extractUserId(jwt), request));
    }
}
