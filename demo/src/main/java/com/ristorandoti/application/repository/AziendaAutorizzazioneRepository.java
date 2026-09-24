package com.ristorandoti.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.AziendaAutorizzazione;

/**
 * Repository Spring Data JPA per l'entità {@link AziendaAutorizzazione}.
 */
public interface AziendaAutorizzazioneRepository extends JpaRepository<AziendaAutorizzazione, Long> {

    boolean existsByAziendaIdAndUserId(Long aziendaId, Long userId);

    void deleteByAziendaIdAndUserId(Long aziendaId, Long userId);

    /** Elenco delle persone autorizzate a gestire un'azienda (di solito poche, niente paginazione). */
    @EntityGraph(attributePaths = "user")
    List<AziendaAutorizzazione> findByAziendaId(Long aziendaId);
}
