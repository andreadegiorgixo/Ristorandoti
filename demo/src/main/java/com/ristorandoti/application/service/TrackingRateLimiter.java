package com.ristorandoti.application.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Rate limiting (Bucket4j) dedicato agli endpoint di tracciamento metriche, per evitare
 * l'inflazione dei contatori (es. un client che chiama ripetutamente l'endpoint di
 * visualizzazione). Bucket in memoria per utente, tramite il modulo {@code bucket4j_jdk17-core}
 * già dichiarato nel progetto (nessuna nuova dipendenza).
 *
 * <p>Semplificazione nota: i bucket restano in una mappa in memoria per la vita dell'istanza
 * (nessuna scadenza/evizione) — accettabile alla scala attuale di una singola istanza; andrebbe
 * sostituito con una cache con TTL (o Bucket4j su Redis, già disponibile come dipendenza per uno
 * scenario multi-istanza) se il numero di utenti diventasse rilevante.</p>
 */
@Component
public class TrackingRateLimiter {

    private final ConcurrentMap<Long, Bucket> bucketPerUtente = new ConcurrentHashMap<>();

    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;

    public TrackingRateLimiter(@Value("${app.rate-limit.tracking.capacity}") long capacity,
                               @Value("${app.rate-limit.tracking.refill-tokens}") long refillTokens,
                               @Value("${app.rate-limit.tracking.refill-period}") Duration refillPeriod) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = refillPeriod;
    }

    /**
     * @param userId utente autenticato che sta chiamando l'endpoint di tracciamento
     * @return {@code true} se la richiesta è consentita, {@code false} se il limite è stato superato
     */
    public boolean tryConsume(Long userId) {
        Bucket bucket = bucketPerUtente.computeIfAbsent(userId, id -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillIntervally(refillTokens, refillPeriod).build())
                .build());
        return bucket.tryConsume(1);
    }
}
