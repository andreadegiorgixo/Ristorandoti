package com.ristorandoti.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.CandidaturaDto;
import com.ristorandoti.application.dto.CandidaturaRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.CandidaturaLavoroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Candidature a un'offerta di lavoro. Tutte le rotte richiedono il JWT.
 *
 * <p>Chiunque può candidarsi (non serve essere dipendente dell'azienda); solo chi ha
 * {@code MANAGE_JOBS} può vedere l'elenco dei candidati.</p>
 */
@RestController
@RequestMapping("/api/aziende/{aziendaId}/offerte-lavoro/{offertaId}/candidature")
@RequiredArgsConstructor
public class CandidaturaLavoroController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final CandidaturaLavoroService candidaturaLavoroService;

    /**
     * @return {@code 201 Created} con la candidatura inviata, {@code 400} se l'offerta è scaduta
     *         o l'utente si è già candidato, {@code 404} se l'offerta non esiste o non appartiene
     *         a questa azienda
     */
    @PostMapping
    public ResponseEntity<CandidaturaDto> candidati(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                     @PathVariable Long offertaId,
                                                     @Valid @RequestBody(required = false) CandidaturaRequestDto request) {
        CandidaturaRequestDto body = request != null ? request : CandidaturaRequestDto.builder().build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidaturaLavoroService.candidati(aziendaId, offertaId, JwtService.extractUserId(jwt), body));
    }

    /**
     * @return {@code 200 OK} con una pagina dei candidati (profilo, data, stato), dal più
     *         recente, {@code 403} se l'utente autenticato non ha {@code MANAGE_JOBS}
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<CandidaturaDto>> lista(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                                  @PathVariable Long offertaId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(
                candidaturaLavoroService.getCandidati(aziendaId, offertaId, JwtService.extractUserId(jwt), page, size));
    }
}
