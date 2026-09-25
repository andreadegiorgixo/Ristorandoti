package com.ristorandoti.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.OffertaLavoroDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.OffertaLavoroService;

import lombok.RequiredArgsConstructor;

/**
 * Ricerca globale delle offerte di lavoro, su tutte le aziende (a differenza di
 * {@link OffertaLavoroController}, che è annidato sotto una singola azienda). Usata dalla
 * ricerca globale in navbar.
 */
@RestController
@RequestMapping("/api/offerte-lavoro")
@RequiredArgsConstructor
public class OffertaLavoroSearchController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final OffertaLavoroService offertaLavoroService;

    /**
     * @param q testo digitato dall'utente; se vuoto la ricerca non restituisce risultati
     * @return {@code 200 OK} con una pagina delle offerte attive corrispondenti, dalla più recente
     */
    @GetMapping("/ricerca")
    public ResponseEntity<PageResponseDto<OffertaLavoroDto>> search(@AuthenticationPrincipal Jwt jwt,
                                                                      @RequestParam(name = "q", defaultValue = "") String q,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(offertaLavoroService.search(q, JwtService.extractUserId(jwt), page, size));
    }
}
