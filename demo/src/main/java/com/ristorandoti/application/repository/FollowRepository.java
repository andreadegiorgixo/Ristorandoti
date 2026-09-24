package com.ristorandoti.application.repository;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.Follow;

/**
 * Repository Spring Data JPA per l'entità {@link Follow}.
 *
 * <p>{@link #findFollowedIds} serve a marcare, in una lista di utenti (follower o following di
 * qualcun altro), quali sono già seguiti dall'utente che fa la richiesta: una sola query in blocco
 * invece di N. Non va chiamato con una collezione vuota (Oracle non accetta {@code IN ()}).</p>
 */
public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);

    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);

    /** Numero di persone che seguono l'utente. */
    long countByFollowedId(Long followedId);

    /** Numero di persone seguite dall'utente. */
    long countByFollowerId(Long followerId);

    /** Elenco paginato di chi segue l'utente, dal più recente. Carica anche {@code follower}. */
    @EntityGraph(attributePaths = "follower")
    Page<Follow> findByFollowedId(Long followedId, Pageable pageable);

    /** Elenco paginato di chi è seguito dall'utente, dal più recente. Carica anche {@code followed}. */
    @EntityGraph(attributePaths = "followed")
    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);

    /**
     * @param followerId utente di cui verificare i follow
     * @param followedIds id degli utenti da verificare (non vuota)
     * @return gli id, tra quelli indicati, che {@code followerId} segue già
     */
    @Query("select f.followed.id from Follow f where f.follower.id = :followerId and f.followed.id in :followedIds")
    Set<Long> findFollowedIds(@Param("followerId") Long followerId, @Param("followedIds") Collection<Long> followedIds);
}
