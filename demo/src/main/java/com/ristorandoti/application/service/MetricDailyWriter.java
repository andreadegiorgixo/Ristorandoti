package com.ristorandoti.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.entity.MetricaGiornaliera;
import com.ristorandoti.application.repository.AziendaMetricDailyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Unico punto di scrittura sull'aggregato giornaliero delle metriche. È un bean a parte (non un
 * metodo privato di {@link MetricsService}) proprio perché {@link Transactional} con
 * {@link Propagation#REQUIRES_NEW} funziona solo attraverso una vera chiamata al proxy Spring: se
 * fosse un metodo privato richiamato internamente (self-invocation), l'annotazione verrebbe
 * ignorata e l'incremento finirebbe nella stessa transazione dell'azione che lo ha generato
 * (follow, like, ricerca, ...), rischiando di far fallire quell'azione per un problema di sola
 * reportistica.
 */
@Service
@RequiredArgsConstructor
public class MetricDailyWriter {

    private final AziendaMetricDailyRepository repository;

    /**
     * Incrementa (o inizializza) la metrica del giorno corrente per un'azienda, in una
     * transazione indipendente da quella del chiamante.
     *
     * @param aziendaId azienda a cui appartiene la metrica
     * @param giorno    giorno a cui appartiene la metrica
     * @param metrica   metrica da incrementare
     * @param delta     variazione da applicare (può essere negativa)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementa(Long aziendaId, LocalDate giorno, MetricaGiornaliera metrica, long delta) {
        repository.incrementa(aziendaId, giorno, metrica.name(), delta);
    }
}
