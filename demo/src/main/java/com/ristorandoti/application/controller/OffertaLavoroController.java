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

import com.ristorandoti.application.dto.OffertaLavoroDto;
import com.ristorandoti.application.dto.OffertaLavoroRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.OffertaLavoroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST delle offerte di lavoro di un'azienda. Tutte le rotte richiedono il JWT.
 *
 * <p>Pubblicazione ed eliminazione sono riservate al proprietario e alle persone autorizzate
 * (vedi {@link com.ristorandoti.application.service.AziendaService#ensureManageable}); la
 * lettura è aperta a qualsiasi utente autenticato.</p>
 */
@RestController
@RequestMapping("/api/aziende/{aziendaId}/offerte-lavoro")
@RequiredArgsConstructor
public class OffertaLavoroController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final OffertaLavoroService offertaLavoroService;

    /**
     * @param aziendaId id dell'azienda
     * @return {@code 200 OK} con una pagina delle offerte di lavoro attive, {@code 404} se
     *         l'azienda non esiste
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<OffertaLavoroDto>> getByAzienda(@PathVariable Long aziendaId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(offertaLavoroService.getByAzienda(aziendaId, page, size));
    }

    /**
     * @param aziendaId id dell'azienda
     * @param request   dati dell'offerta, già validati
     * @return {@code 201 Created} con l'offerta pubblicata, {@code 400} se l'azienda ha già
     *         3 offerte attive, {@code 403} se l'utente non è proprietario né autorizzato
     */
    @PostMapping
    public ResponseEntity<OffertaLavoroDto> create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                    @Valid @RequestBody OffertaLavoroRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offertaLavoroService.create(aziendaId, JwtService.extractUserId(jwt), request));
    }

    /**
     * Chiude (elimina) un'offerta di lavoro, liberando uno slot per una nuova.
     *
     * @param aziendaId id dell'azienda (solo per coerenza della rotta: l'autorizzazione si basa
     *                  sull'azienda effettiva dell'offerta)
     * @param offertaId id dell'offerta da chiudere
     * @return {@code 204 No Content}, {@code 403} se l'utente non è proprietario né autorizzato,
     *         {@code 404} se l'offerta non esiste
     */
    @DeleteMapping("/{offertaId}")
    public ResponseEntity<Void> chiudi(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                        @PathVariable Long offertaId) {
        offertaLavoroService.chiudi(offertaId, JwtService.extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }
}
