package com.ristorandoti.application.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.Azienda;

import jakarta.persistence.LockModeType;

/**
 * Repository Spring Data JPA per l'entità {@link Azienda}.
 *
 * <p>Gli {@link EntityGraph} caricano il proprietario nella stessa query, evitando una query
 * in più per ogni azienda (problema N+1), come già fatto in {@code PostRepository}.</p>
 */
public interface AziendaRepository extends JpaRepository<Azienda, Long> {

    /**
     * Come {@link #findById(Object)}, ma con lock pessimistico sulla riga: serializza le
     * operazioni concorrenti che devono contare righe collegate all'azienda in modo affidabile
     * (es. limite di offerte di lavoro attive, invariante "sempre almeno un Admin").
     *
     * @param id id dell'azienda
     * @return l'azienda, con la riga bloccata fino al termine della transazione corrente
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Azienda a WHERE a.id = :id")
    Optional<Azienda> findByIdForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"proprietario", "servizi"})
    Page<Azienda> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"proprietario", "servizi"})
    Page<Azienda> findByProprietarioId(Long proprietarioId, Pageable pageable);

    /** Ricerca per nome (contiene, senza distinzione maiuscole/minuscole): usata dal tipo-mentre-scrivi. */
    @EntityGraph(attributePaths = {"proprietario", "servizi"})
    Page<Azienda> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
