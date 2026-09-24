package com.ristorandoti.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.OffertaLavoro;

/**
 * Repository Spring Data JPA per l'entità {@link OffertaLavoro}.
 */
public interface OffertaLavoroRepository extends JpaRepository<OffertaLavoro, Long> {

    @EntityGraph(attributePaths = {"azienda", "autore"})
    Page<OffertaLavoro> findByAziendaId(Long aziendaId, Pageable pageable);

    /** Usato per far rispettare il limite di 3 offerte contemporanee (vedi {@code OffertaLavoroService}). */
    long countByAziendaId(Long aziendaId);
}
