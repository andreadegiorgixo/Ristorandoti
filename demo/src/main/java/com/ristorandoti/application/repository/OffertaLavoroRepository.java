package com.ristorandoti.application.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.entity.StatoOffertaLavoro;

/**
 * Repository Spring Data JPA per l'entità {@link OffertaLavoro}.
 */
public interface OffertaLavoroRepository extends JpaRepository<OffertaLavoro, Long> {

    /** Vista Dashboard: tutte le offerte dell'azienda (attive e scadute), per lo storico. */
    @EntityGraph(attributePaths = {"azienda", "autore"})
    Page<OffertaLavoro> findByAziendaId(Long aziendaId, Pageable pageable);

    /** Vista pubblica: solo le offerte non ancora scadute, ricalcolato a lettura (non fidandosi solo di {@code stato}). */
    @EntityGraph(attributePaths = {"azienda", "autore"})
    Page<OffertaLavoro> findByAziendaIdAndDataScadenzaAfter(Long aziendaId, Instant now, Pageable pageable);

    /**
     * Conteggio usato per far rispettare il limite di offerte contemporanee (vedi
     * {@code OffertaLavoroService.create}): ricalcolato a lettura sulla scadenza effettiva, non
     * sulla colonna {@code stato} (che potrebbe non essere ancora stata aggiornata dallo scheduler).
     */
    long countByAziendaIdAndDataScadenzaAfter(Long aziendaId, Instant now);

    /** Marca come scadute le offerte oltre {@code dataScadenza}: usato solo dallo scheduler di pulizia. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE OffertaLavoro o SET o.stato = :nuovo WHERE o.dataScadenza < :now AND o.stato = :attuale")
    int marcaScadute(@Param("now") Instant now, @Param("attuale") StatoOffertaLavoro attuale, @Param("nuovo") StatoOffertaLavoro nuovo);

    /** Offerte scadute da più di {@code JOB_EXPIRED_RETENTION_DAYS}: usato solo dallo scheduler di pulizia. */
    List<OffertaLavoro> findByDataScadenzaBefore(Instant soglia);
}
