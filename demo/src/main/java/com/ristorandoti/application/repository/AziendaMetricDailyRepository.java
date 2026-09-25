package com.ristorandoti.application.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.AziendaMetricDaily;
import com.ristorandoti.application.entity.MetricaGiornaliera;

/** Repository Spring Data JPA per l'aggregato giornaliero delle metriche ({@link AziendaMetricDaily}). */
public interface AziendaMetricDailyRepository extends JpaRepository<AziendaMetricDaily, Long> {

    /**
     * Upsert atomico (una sola andata e ritorno al database, senza il classico
     * "leggi poi scrivi" che soffrirebbe di race condition sotto scritture concorrenti):
     * incrementa il valore esistente per {@code (aziendaId, giorno, metrica)}, oppure crea la
     * riga con {@code delta} come valore iniziale se non esiste ancora.
     *
     * @param aziendaId azienda a cui appartiene la metrica
     * @param giorno    giorno a cui appartiene la metrica
     * @param metrica   nome della metrica ({@link com.ristorandoti.application.entity.MetricaGiornaliera})
     * @param delta     variazione da applicare (può essere negativa, es. per un unfollow)
     */
    @Modifying
    @Query(value = """
            MERGE INTO azienda_metric_daily m
            USING (SELECT ?1 AS azienda_id, ?2 AS giorno, ?3 AS metrica FROM dual) src
            ON (m.azienda_id = src.azienda_id AND m.giorno = src.giorno AND m.metrica = src.metrica)
            WHEN MATCHED THEN UPDATE SET m.valore = m.valore + ?4
            WHEN NOT MATCHED THEN INSERT (azienda_id, giorno, metrica, valore)
                VALUES (src.azienda_id, src.giorno, src.metrica, ?4)
            """, nativeQuery = true)
    void incrementa(Long aziendaId, LocalDate giorno, String metrica, long delta);

    /** Righe di una singola metrica "di flusso" (visualizzatori, ricerche) in un intervallo, per il grafico e il totale di periodo. */
    List<AziendaMetricDaily> findByAziendaIdAndMetricaAndGiornoBetweenOrderByGiornoAsc(
            Long aziendaId, MetricaGiornaliera metrica, LocalDate dal, LocalDate al);

    /**
     * Tutte le righe di una metrica "di stock" (follower, like: variazioni +1/-1) fino a una data,
     * per calcolare il valore cumulativo in qualunque punto del tempo richiesto.
     */
    List<AziendaMetricDaily> findByAziendaIdAndMetricaAndGiornoLessThanEqualOrderByGiornoAsc(
            Long aziendaId, MetricaGiornaliera metrica, LocalDate al);

    /** @return il primo giorno con dati per l'azienda (qualunque metrica), per dichiarare "dati disponibili da...". */
    @Query("SELECT MIN(m.giorno) FROM AziendaMetricDaily m WHERE m.azienda.id = :aziendaId")
    Optional<LocalDate> findPrimoGiorno(@Param("aziendaId") Long aziendaId);
}
