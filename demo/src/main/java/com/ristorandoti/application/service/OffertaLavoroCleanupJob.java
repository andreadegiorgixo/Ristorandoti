package com.ristorandoti.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.config.DashboardProperties;
import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.entity.StatoOffertaLavoro;
import com.ristorandoti.application.repository.OffertaLavoroRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pulizia notturna delle offerte di lavoro scadute. Non è l'unico meccanismo che fa rispettare la
 * scadenza: ogni lettura ricalcola già lo stato effettivo confrontando {@code dataScadenza} con
 * l'istante corrente (vedi {@link com.ristorandoti.application.mapper.OffertaLavoroMapper}), quindi
 * un'offerta scaduta sparisce subito dalla vista pubblica anche se questo job non è ancora passato.
 * Questo scheduler serve solo a: (1) aggiornare la colonna {@code stato} per lo storico Dashboard,
 * (2) cancellare fisicamente le offerte scadute da più di
 * {@code app.dashboard.job-expired-retention-days} (le candidature collegate seguono a cascata).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OffertaLavoroCleanupJob {

    private final OffertaLavoroRepository offertaLavoroRepository;
    private final DashboardProperties dashboardProperties;

    /** Ogni notte alle 3:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void eseguiPulizia() {
        Instant now = Instant.now();

        int marcate = offertaLavoroRepository.marcaScadute(now, StatoOffertaLavoro.ATTIVA, StatoOffertaLavoro.SCADUTA);

        Instant sogliaRetention = now.minus(dashboardProperties.getJobExpiredRetentionDays(), ChronoUnit.DAYS);
        List<OffertaLavoro> daEliminare = offertaLavoroRepository.findByDataScadenzaBefore(sogliaRetention);
        if (!daEliminare.isEmpty()) {
            offertaLavoroRepository.deleteAll(daEliminare);
        }

        log.info("Pulizia offerte di lavoro: {} marcate come scadute, {} eliminate (oltre {} giorni dalla scadenza)",
                marcate, daEliminare.size(), dashboardProperties.getJobExpiredRetentionDays());
    }
}
