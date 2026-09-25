package com.ristorandoti.application.controller;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.DashboardMetricheDto;
import com.ristorandoti.application.dto.MetricaDashboard;
import com.ristorandoti.application.exception.TooManyRequestsException;
import com.ristorandoti.application.security.JwtService;
import com.ristorandoti.application.service.MetricsQueryService;
import com.ristorandoti.application.service.MetricsService;
import com.ristorandoti.application.service.TrackingRateLimiter;

import lombok.RequiredArgsConstructor;

/**
 * Tracciamento e lettura delle metriche della pagina aziendale. Tutte le rotte richiedono il JWT.
 */
@RestController
@RequestMapping("/api/aziende/{aziendaId}")
@RequiredArgsConstructor
public class AziendaMetricheController {

    private final MetricsService metricsService;
    private final MetricsQueryService metricsQueryService;
    private final TrackingRateLimiter trackingRateLimiter;

    /**
     * Registra una visualizzazione della pagina pubblica. Va chiamato solo dalla vista pubblica
     * (mai dalla Dashboard, altrimenti chi gestisce la pagina finirebbe per gonfiare le proprie
     * statistiche): {@link MetricsService#recordPageView} esclude comunque chi ha accesso alla
     * Dashboard, come ulteriore rete di sicurezza.
     *
     * @param aziendaId pagina visualizzata
     * @return {@code 204 No Content}, {@code 429} se l'utente ha superato il limite di richieste
     */
    @PostMapping("/metriche/eventi/visualizzazione")
    public ResponseEntity<Void> registraVisualizzazione(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId) {
        Long userId = JwtService.extractUserId(jwt);
        if (!trackingRateLimiter.tryConsume(userId)) {
            throw new TooManyRequestsException("Troppe richieste di tracciamento: riprova tra qualche istante");
        }
        metricsService.recordPageView(aziendaId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * @param aziendaId pagina di cui leggere le metriche
     * @param dal       inizio del periodo (incluso)
     * @param al        fine del periodo (incluso)
     * @param metriche  metriche richieste (una o più)
     * @return {@code 200 OK} con una serie per ogni metrica richiesta, {@code 403} se l'utente
     *         autenticato non ha {@code VIEW_DASHBOARD}
     */
    @GetMapping("/dashboard/metriche")
    public ResponseEntity<DashboardMetricheDto> metriche(@AuthenticationPrincipal Jwt jwt, @PathVariable Long aziendaId,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dal,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate al,
                                                          @RequestParam Set<MetricaDashboard> metriche) {
        return ResponseEntity.ok(
                metricsQueryService.getMetriche(aziendaId, JwtService.extractUserId(jwt), dal, al, metriche));
    }
}
