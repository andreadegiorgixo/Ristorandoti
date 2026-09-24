package com.ristorandoti.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.FollowStatusDto;
import com.ristorandoti.application.dto.FollowUserDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.FollowService;

import lombok.RequiredArgsConstructor;

/**
 * Controller REST del "follow" tra utenti. Tutte le rotte richiedono il JWT.
 *
 * <p>L'utente che segue/smette di seguire è sempre ricavato dalla claim {@code uid} del token,
 * mai da un parametro della richiesta: così nessuno può far seguire un altro utente al posto suo.</p>
 */
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final FollowService followService;

    /**
     * @param userId utente da iniziare a seguire
     * @return {@code 200 OK} con lo stato aggiornato, {@code 400} per l'auto-follow,
     *         {@code 404} se l'utente non esiste
     */
    @PostMapping("/{userId}")
    public ResponseEntity<FollowStatusDto> follow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        return ResponseEntity.ok(followService.follow(JwtService.extractUserId(jwt), userId));
    }

    /**
     * @param userId utente da smettere di seguire
     * @return {@code 200 OK} con lo stato aggiornato (idempotente)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<FollowStatusDto> unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long userId) {
        return ResponseEntity.ok(followService.unfollow(JwtService.extractUserId(jwt), userId));
    }

    /**
     * @param userId utente di cui elencare i follower
     * @return {@code 200 OK} con una pagina di chi segue {@code userId}, {@code 404} se non esiste
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<PageResponseDto<FollowUserDto>> getFollowers(@AuthenticationPrincipal Jwt jwt,
                                                                        @PathVariable Long userId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(followService.getFollowers(userId, JwtService.extractUserId(jwt), page, size));
    }

    /**
     * @param userId utente di cui elencare i seguiti
     * @return {@code 200 OK} con una pagina di chi è seguito da {@code userId}, {@code 404} se non esiste
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<PageResponseDto<FollowUserDto>> getFollowing(@AuthenticationPrincipal Jwt jwt,
                                                                        @PathVariable Long userId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(followService.getFollowing(userId, JwtService.extractUserId(jwt), page, size));
    }
}
