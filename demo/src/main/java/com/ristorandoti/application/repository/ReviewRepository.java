package com.ristorandoti.application.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.Review;

/**
 * Repository Spring Data JPA per l'entità {@link Review}.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByAutoreIdAndDestinatarioId(Long autoreId, Long destinatarioId);

    void deleteByAutoreIdAndDestinatarioId(Long autoreId, Long destinatarioId);

    /** Recensioni ricevute da un utente, dalla più recente. Carica anche {@code autore}. */
    @EntityGraph(attributePaths = "autore")
    Page<Review> findByDestinatarioId(Long destinatarioId, Pageable pageable);

    long countByDestinatarioId(Long destinatarioId);

    /**
     * @param destinatarioId utente recensito
     * @return la media dei voti ricevuti, {@code null} se non ha ancora recensioni
     */
    @Query("select avg(r.valutazione) from Review r where r.destinatario.id = :destinatarioId")
    Double averageValutazioneByDestinatarioId(@Param("destinatarioId") Long destinatarioId);
}
