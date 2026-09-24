package com.ristorandoti.application.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.ristorandoti.application.dto.AziendaAutorizzazioneDto;
import com.ristorandoti.application.dto.AziendaDto;
import com.ristorandoti.application.dto.AziendaPersonaDto;
import com.ristorandoti.application.dto.AziendaRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.AziendaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST delle aziende. Tutte le rotte richiedono il JWT.
 *
 * <p>Qualsiasi utente autenticato può creare un'azienda, senza distinzione di ruolo. Il
 * proprietario è sempre ricavato dalla claim {@code uid} del token, mai da un parametro della
 * richiesta. Solo il proprietario può modificare o eliminare la propria azienda
 * ({@link AziendaService#update}/{@link AziendaService#delete} rispondono {@code 403} altrimenti).</p>
 */
@RestController
@RequestMapping("/api/aziende")
@RequiredArgsConstructor
public class AziendaController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final AziendaService aziendaService;

    /**
     * @param request dati dell'azienda, già validati
     * @return {@code 201 Created} con l'azienda creata
     */
    @PostMapping
    public ResponseEntity<AziendaDto> create(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody AziendaRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aziendaService.create(JwtService.extractUserId(jwt), request));
    }

    /**
     * @param id id dell'azienda
     * @return {@code 200 OK} con l'azienda, {@code 404} se non esiste
     */
    @GetMapping("/{id}")
    public ResponseEntity<AziendaDto> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(aziendaService.getById(id, JwtService.extractUserId(jwt)));
    }

    /**
     * @return {@code 200 OK} con una pagina di tutte le aziende, dalla più recente
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<AziendaDto>> getAll(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(aziendaService.getAll(page, size));
    }

    /**
     * @param userId proprietario di cui leggere le aziende
     * @return {@code 200 OK} con una pagina delle sue aziende, {@code 404} se l'utente non esiste
     */
    @GetMapping("/utente/{userId}")
    public ResponseEntity<PageResponseDto<AziendaDto>> getByUser(@PathVariable Long userId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(aziendaService.getByProprietario(userId, page, size));
    }

    /**
     * Ricerca le aziende per nome, usata dal campo di ricerca quando si collega un'esperienza
     * a un'azienda esistente.
     *
     * @param q    testo digitato dall'utente; se vuoto la ricerca non restituisce risultati
     * @return {@code 200 OK} con una pagina delle aziende corrispondenti, in ordine alfabetico
     */
    @GetMapping("/ricerca")
    public ResponseEntity<PageResponseDto<AziendaDto>> search(@RequestParam(name = "q", defaultValue = "") String q,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(aziendaService.search(q, page, size));
    }

    /**
     * @param id      id dell'azienda da modificare
     * @param request nuovi valori, già validati
     * @return {@code 200 OK} con l'azienda aggiornata, {@code 403} se non si è il proprietario,
     *         {@code 404} se non esiste
     */
    @PutMapping("/{id}")
    public ResponseEntity<AziendaDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                             @Valid @RequestBody AziendaRequestDto request) {
        return ResponseEntity.ok(aziendaService.update(id, JwtService.extractUserId(jwt), request));
    }

    /**
     * @param id id dell'azienda da eliminare
     * @return {@code 204 No Content}, {@code 403} se non si è il proprietario, {@code 404} se non esiste
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        aziendaService.delete(id, JwtService.extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    /**
     * @param id id dell'azienda
     * @return {@code 200 OK} con una pagina delle persone che lavorano attualmente nell'azienda,
     *         {@code 404} se l'azienda non esiste
     */
    @GetMapping("/{id}/persone")
    public ResponseEntity<PageResponseDto<AziendaPersonaDto>> getPersone(@PathVariable Long id,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(aziendaService.getPersone(id, page, size));
    }

    /**
     * @param id id dell'azienda
     * @return {@code 200 OK} con l'elenco delle persone autorizzate a gestire la pagina aziendale
     *         (proprietario escluso), {@code 403} se l'utente autenticato non è il proprietario,
     *         {@code 404} se l'azienda non esiste
     */
    @GetMapping("/{id}/autorizzazioni")
    public ResponseEntity<List<AziendaAutorizzazioneDto>> getAutorizzazioni(@AuthenticationPrincipal Jwt jwt,
                                                                             @PathVariable Long id) {
        return ResponseEntity.ok(aziendaService.getAutorizzati(id, JwtService.extractUserId(jwt)));
    }

    /**
     * Autorizza un utente a gestire la pagina aziendale (pubblicare post, inserire offerte di
     * lavoro). Idempotente. Solo il proprietario può farlo.
     *
     * @param id     id dell'azienda
     * @param userId utente da autorizzare
     * @return {@code 200 OK} con la persona autorizzata, {@code 403} se l'utente autenticato non
     *         è il proprietario, {@code 404} se l'azienda o l'utente da autorizzare non esistono
     */
    @PostMapping("/{id}/autorizzazioni/{userId}")
    public ResponseEntity<AziendaAutorizzazioneDto> autorizza(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(aziendaService.autorizza(id, JwtService.extractUserId(jwt), userId));
    }

    /**
     * Revoca l'autorizzazione di un utente a gestire la pagina aziendale. Idempotente. Solo il
     * proprietario può farlo.
     *
     * @param id     id dell'azienda
     * @param userId utente da revocare
     * @return {@code 204 No Content}, {@code 403} se l'utente autenticato non è il proprietario,
     *         {@code 404} se l'azienda non esiste
     */
    @DeleteMapping("/{id}/autorizzazioni/{userId}")
    public ResponseEntity<Void> revoca(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @PathVariable Long userId) {
        aziendaService.revoca(id, JwtService.extractUserId(jwt), userId);
        return ResponseEntity.noContent().build();
    }
}
