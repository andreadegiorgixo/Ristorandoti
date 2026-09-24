package com.ristorandoti.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.AziendaFollow;

/**
 * Repository Spring Data JPA per l'entità {@link AziendaFollow}. Stesso ruolo di
 * {@code FollowRepository}, ma per il follow di una pagina aziendale.
 */
public interface AziendaFollowRepository extends JpaRepository<AziendaFollow, Long> {

    boolean existsByFollowerIdAndAziendaId(Long followerId, Long aziendaId);

    void deleteByFollowerIdAndAziendaId(Long followerId, Long aziendaId);

    /** Numero di persone che seguono la pagina aziendale. */
    long countByAziendaId(Long aziendaId);
}
