package com.ristorandoti.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.CreatePostRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.PostDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST dei post e del feed. Tutte le rotte richiedono il JWT.
 *
 * <p>Le liste sono paginate: {@code ?page=0&size=20} (default), dal post più recente.
 * Per lo scroll infinito il client richiede la pagina successiva finché {@code last} è {@code false}.</p>
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final PostService postService;

    /**
     * @return {@code 200 OK} con una pagina del feed della community
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<PostDto>> getFeed(@AuthenticationPrincipal Jwt jwt,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(postService.getFeed(JwtService.extractUserId(jwt), page, size));
    }

    /**
     * @param userId autore di cui leggere i post
     * @return {@code 200 OK} con una pagina dei suoi post, {@code 404} se l'utente non esiste
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponseDto<PostDto>> getPostsByUser(@AuthenticationPrincipal Jwt jwt,
                                                                   @PathVariable Long userId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(postService.getPostsByUser(userId, JwtService.extractUserId(jwt), page, size));
    }

    /**
     * @param request testo e/o URL della foto
     * @return {@code 201 Created} con il post pubblicato
     */
    @PostMapping
    public ResponseEntity<PostDto> createPost(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody CreatePostRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(JwtService.extractUserId(jwt), request));
    }

    /**
     * @param postId post a cui mettere like (idempotente)
     * @return {@code 200 OK} con il post aggiornato, {@code 404} se non esiste
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostDto> like(@AuthenticationPrincipal Jwt jwt, @PathVariable Long postId) {
        return ResponseEntity.ok(postService.like(postId, JwtService.extractUserId(jwt)));
    }

    /**
     * @param postId post da cui togliere il like (idempotente)
     * @return {@code 200 OK} con il post aggiornato, {@code 404} se non esiste
     */
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<PostDto> unlike(@AuthenticationPrincipal Jwt jwt, @PathVariable Long postId) {
        return ResponseEntity.ok(postService.unlike(postId, JwtService.extractUserId(jwt)));
    }
}
