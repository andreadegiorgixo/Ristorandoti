package com.ristorandoti.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.CandidaturaLavoro;

/** Repository Spring Data JPA per le candidature a un'offerta di lavoro ({@link CandidaturaLavoro}). */
public interface CandidaturaLavoroRepository extends JpaRepository<CandidaturaLavoro, Long> {

    boolean existsByOffertaIdAndCandidatoId(Long offertaId, Long candidatoId);

    @EntityGraph(attributePaths = "candidato")
    Page<CandidaturaLavoro> findByOffertaIdOrderByDataCreazioneDesc(Long offertaId, Pageable pageable);
}
