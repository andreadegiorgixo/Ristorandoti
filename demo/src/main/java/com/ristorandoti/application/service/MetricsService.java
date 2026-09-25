package com.ristorandoti.application.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.ristorandoti.application.config.DashboardProperties;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.MetricaGiornaliera;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Punto unico di scrittura degli eventi che alimentano le metriche della Home Dashboard
 * (visualizzatori unici, follower, like, ricerche). Aggrega direttamente in
 * {@code azienda_metric_daily} tramite {@link MetricDailyWriter} (nessuno storico grezzo degli
 * eventi viene conservato) e usa Redis solo per la deduplica a breve termine (visualizzatori
 * unici per utente/giorno, apparizioni nei risultati di ricerca per utente/giorno/query).
 *
 * <p><b>Le metriche non devono mai far fallire l'azione dell'utente che le genera</b> (seguire
 * un'azienda, mettere like, cercare): ogni metodo pubblico intercetta qualsiasi eccezione e si
 * limita a loggarla, dato che {@link MetricDailyWriter} gira già in una transazione separata.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final String VALORE_DEDUP = "1";

    private final MetricDailyWriter writer;
    private final StringRedisTemplate redisTemplate;
    private final AziendaPermissionService aziendaPermissionService;
    private final DashboardProperties dashboardProperties;

    /**
     * Registra una visualizzazione della pagina pubblica dell'azienda. Esclude chi sta gestendo
     * la pagina (proprietario o dipendente con almeno una capability sulla Dashboard) e deduplica
     * per utente e giorno: va chiamato solo dalla vista pubblica, mai dalla Dashboard stessa.
     *
     * @param aziendaId azienda visualizzata
     * @param viewerId  utente che visualizza la pagina
     */
    public void recordPageView(Long aziendaId, Long viewerId) {
        try {
            if (aziendaPermissionService.hasCapability(aziendaId, viewerId, Capability.VIEW_DASHBOARD)) {
                return;
            }
            String chiave = "metrics:view:dedup:%d:%s:%d".formatted(aziendaId, LocalDate.now(), viewerId);
            if (primaVoltaOggi(chiave)) {
                writer.incrementa(aziendaId, LocalDate.now(), MetricaGiornaliera.VISUALIZZATORI_UNICI, 1);
            }
        } catch (RuntimeException e) {
            log.warn("Impossibile registrare la visualizzazione della pagina dell'azienda {}: {}", aziendaId, e.getMessage());
        }
    }

    /**
     * Registra un'apparizione dell'azienda nei risultati di una ricerca del portale. Centralizzato
     * in un solo punto ({@code AziendaService.search}) come richiesto dal brief, così la
     * definizione di "Ricerche" resta modificabile in un unico posto (es. passando al conteggio
     * dei soli click, vedi {@link DashboardProperties.SearchMetricMode#CLICK}).
     *
     * @param aziendaId azienda apparsa nei risultati
     * @param viewerId  utente che ha effettuato la ricerca
     * @param query     testo cercato (usato solo per la deduplica, non conservato)
     */
    public void recordSearchAppearance(Long aziendaId, Long viewerId, String query) {
        try {
            String queryNormalizzata = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));
            String chiave = "metrics:search:dedup:%d:%s:%d:%d"
                    .formatted(aziendaId, LocalDate.now(), viewerId, queryNormalizzata.hashCode());
            if (primaVoltaOggi(chiave)) {
                writer.incrementa(aziendaId, LocalDate.now(), MetricaGiornaliera.RICERCHE, 1);
            }
        } catch (RuntimeException e) {
            log.warn("Impossibile registrare l'apparizione nei risultati di ricerca dell'azienda {}: {}", aziendaId, e.getMessage());
        }
    }

    /**
     * @param aziendaId azienda seguita/non più seguita
     * @param delta     {@code +1} per un follow, {@code -1} per un unfollow
     */
    public void recordFollowDelta(Long aziendaId, long delta) {
        try {
            writer.incrementa(aziendaId, LocalDate.now(), MetricaGiornaliera.FOLLOWER_DELTA, delta);
        } catch (RuntimeException e) {
            log.warn("Impossibile registrare la variazione di follower dell'azienda {}: {}", aziendaId, e.getMessage());
        }
    }

    /**
     * @param aziendaId azienda proprietaria del post messo/tolto like (solo post di pagina aziendale)
     * @param delta     {@code +1} per un like, {@code -1} per un unlike
     */
    public void recordLikeDelta(Long aziendaId, long delta) {
        try {
            writer.incrementa(aziendaId, LocalDate.now(), MetricaGiornaliera.LIKE_DELTA, delta);
        } catch (RuntimeException e) {
            log.warn("Impossibile registrare la variazione di like dell'azienda {}: {}", aziendaId, e.getMessage());
        }
    }

    /**
     * @param chiave chiave di deduplica (già comprensiva di azienda/giorno/utente/eventuale query)
     * @return {@code true} solo la prima volta che questa chiave viene vista nella finestra di
     *         deduplica configurata ({@code app.dashboard.view-dedup-ttl-hours})
     */
    private boolean primaVoltaOggi(String chiave) {
        Boolean impostata = redisTemplate.opsForValue().setIfAbsent(chiave, VALORE_DEDUP,
                Duration.ofHours(dashboardProperties.getViewDedupTtlHours()));
        return Boolean.TRUE.equals(impostata);
    }
}
