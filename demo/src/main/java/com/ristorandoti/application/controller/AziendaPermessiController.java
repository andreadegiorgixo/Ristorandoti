package com.ristorandoti.application.controller;

import java.util.List;

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

import com.ristorandoti.application.dto.AssegnaRuoloRequestDto;
import com.ristorandoti.application.dto.AziendaAuditLogDto;
import com.ristorandoti.application.dto.AziendaDipendenteRuoliDto;
import com.ristorandoti.application.dto.AziendaPermessiCorrentiDto;
import com.ristorandoti.application.dto.AziendaRuoloDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.AziendaRuoloCodice;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.AziendaPermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestione permessi (ruoli/capability) della Dashboard aziendale. Tutte le rotte richiedono il
 * JWT; le mutazioni e la lettura dell'elenco dipendenti/audit richiedono
 * {@link com.ristorandoti.application.entity.Capability#MANAGE_PERMISSIONS}, verificato
 * direttamente in {@link AziendaPermissionService} ({@code 403} altrimenti).
 */
@RestController
@RequestMapping("/api/aziende/{aziendaId}/dashboard/permessi")
@RequiredArgsConstructor
public class AziendaPermessiController {

    private static final String DEFAULT_PAGE_SIZE = "20";

    private final AziendaPermissionService permissionService;

    /**
     * @param aziendaId id dell'azienda
     * @return {@code 200 OK} con le capability e i ruoli dell'utente autenticato su questa azienda
     */
    @GetMapping("/correnti")
    public ResponseEntity<AziendaPermessiCorrentiDto> permessiCorrenti(@AuthenticationPrincipal Jwt jwt,
                                                                        @PathVariable Long aziendaId) {
        return ResponseEntity.ok(permissionService.getPermessiCorrenti(aziendaId, JwtService.extractUserId(jwt)));
    }

    /**
     * @return {@code 200 OK} con il catalogo dei ruoli assegnabili e le capability che comportano
     *         (legenda mostrata nella Dashboard, non richiede permessi particolari)
     */
    @GetMapping("/ruoli")
    public ResponseEntity<List<AziendaRuoloDto>> catalogoRuoli() {
        return ResponseEntity.ok(permissionService.catalogoRuoli());
    }

    /**
     * @param aziendaId id dell'azienda
     * @return {@code 200 OK} con i dipendenti attualmente assunti e i loro ruoli attivi,
     *         {@code 403} se l'utente autenticato non ha {@code MANAGE_PERMISSIONS}
     */
    @GetMapping
    public ResponseEntity<List<AziendaDipendenteRuoliDto>> dipendenti(@AuthenticationPrincipal Jwt jwt,
                                                                       @PathVariable Long aziendaId) {
        return ResponseEntity.ok(permissionService.listaDipendentiConRuoli(aziendaId, JwtService.extractUserId(jwt)));
    }

    /**
     * Assegna un ruolo a un dipendente attualmente assunto. Idempotente.
     *
     * @param aziendaId id dell'azienda
     * @param userId    dipendente a cui assegnare il ruolo
     * @param request   ruolo da assegnare, già validato
     * @return {@code 204 No Content}, {@code 400} se il destinatario non è assunto o è già
     *         proprietario, {@code 403} se l'utente autenticato non ha {@code MANAGE_PERMISSIONS}
     */
    @PostMapping("/{userId}")
    public ResponseEntity<Void> assegna(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                        @PathVariable Long userId, @Valid @RequestBody AssegnaRuoloRequestDto request) {
        permissionService.assignRole(aziendaId, JwtService.extractUserId(jwt), userId, request.getRuolo());
        return ResponseEntity.noContent().build();
    }

    /**
     * Revoca un ruolo attivo di un dipendente. Idempotente.
     *
     * @param aziendaId id dell'azienda
     * @param userId    dipendente a cui revocare il ruolo
     * @param ruolo     ruolo da revocare
     * @return {@code 204 No Content}, {@code 403} se l'utente autenticato non ha {@code MANAGE_PERMISSIONS}
     */
    @DeleteMapping("/{userId}/{ruolo}")
    public ResponseEntity<Void> revoca(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                       @PathVariable Long userId, @PathVariable AziendaRuoloCodice ruolo) {
        permissionService.revokeRole(aziendaId, JwtService.extractUserId(jwt), userId, ruolo);
        return ResponseEntity.noContent().build();
    }

    /**
     * @param aziendaId id dell'azienda
     * @return {@code 200 OK} con una pagina del log di audit (chi/cosa/quando), dalla più recente,
     *         {@code 403} se l'utente autenticato non ha {@code MANAGE_PERMISSIONS}
     */
    @GetMapping("/audit")
    public ResponseEntity<PageResponseDto<AziendaAuditLogDto>> audit(@AuthenticationPrincipal Jwt jwt,
                                                                      @PathVariable Long aziendaId,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(permissionService.getAuditLog(aziendaId, JwtService.extractUserId(jwt), page, size));
    }
}
