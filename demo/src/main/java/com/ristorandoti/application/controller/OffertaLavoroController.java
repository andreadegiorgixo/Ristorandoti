package com.ristorandoti.application.controller;

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
 * <p>Pubblicazione, modifica ed eliminazione richiedono {@code MANAGE_JOBS} (vedi
 * {@link com.ristorandoti.application.service.AziendaPermissionService}); la lettura della vista
 * pubblica è aperta a qualsiasi utente autenticato, quella Dashboard richiede {@code VIEW_DASHBOARD}
 * (vedi {@link OffertaLavoroService#getByAziendaDashboard}, esposta da questo stesso controller).</p>
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
    public ResponseEntity<PageResponseDto<OffertaLavoroDto>> getByAzienda(@AuthenticationPrincipal Jwt jwt,
                                                                           @PathVariable Long aziendaId,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(offertaLavoroService.getByAzienda(aziendaId, JwtService.extractUserId(jwt), page, size));
    }

    /**
     * @param aziendaId id dell'azienda
     * @return {@code 200 OK} con una pagina di tutte le offerte (attive e scadute), {@code 403}
     *         se l'utente autenticato non ha {@code VIEW_DASHBOARD}
     */
    @GetMapping("/dashboard")
    public ResponseEntity<PageResponseDto<OffertaLavoroDto>> getByAziendaDashboard(@AuthenticationPrincipal Jwt jwt,
                                                                                    @PathVariable Long aziendaId,
                                                                                    @RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(
                offertaLavoroService.getByAziendaDashboard(aziendaId, JwtService.extractUserId(jwt), page, size));
    }

    /**
     * @param aziendaId id dell'azienda
     * @param request   dati dell'offerta, già validati
     * @return {@code 201 Created} con l'offerta pubblicata, {@code 400} se l'azienda ha già
     *         raggiunto il limite di offerte attive, {@code 403} se l'utente non ha {@code MANAGE_JOBS}
     */
    @PostMapping
    public ResponseEntity<OffertaLavoroDto> create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                    @Valid @RequestBody OffertaLavoroRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offertaLavoroService.create(aziendaId, JwtService.extractUserId(jwt), request));
    }

    /**
     * @param aziendaId id dell'azienda
     * @param offertaId id dell'offerta da modificare
     * @param request   nuovi titolo/descrizione, già validati
     * @return {@code 200 OK} con l'offerta aggiornata, {@code 403} se l'utente non ha
     *         {@code MANAGE_JOBS}, {@code 404} se l'offerta non esiste o non appartiene a questa
     *         azienda, {@code 400} se l'offerta è scaduta nel frattempo
     */
    @PutMapping("/{offertaId}")
    public ResponseEntity<OffertaLavoroDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                    @PathVariable Long offertaId,
                                                    @Valid @RequestBody OffertaLavoroRequestDto request) {
        return ResponseEntity.ok(
                offertaLavoroService.update(aziendaId, offertaId, JwtService.extractUserId(jwt), request));
    }

    /**
     * Chiude (elimina) un'offerta di lavoro, liberando uno slot per una nuova.
     *
     * @param aziendaId id dell'azienda
     * @param offertaId id dell'offerta da chiudere
     * @return {@code 204 No Content}, {@code 403} se l'utente non ha {@code MANAGE_JOBS},
     *         {@code 404} se l'offerta non esiste o non appartiene a questa azienda
     */
    @DeleteMapping("/{offertaId}")
    public ResponseEntity<Void> chiudi(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                        @PathVariable Long offertaId) {
        offertaLavoroService.chiudi(aziendaId, offertaId, JwtService.extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }
}
