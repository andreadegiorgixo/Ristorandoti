package com.ristorandoti.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.Azienda;

/**
 * Repository Spring Data JPA per l'entità {@link Azienda}.
 *
 * <p>Gli {@link EntityGraph} caricano il proprietario nella stessa query, evitando una query
 * in più per ogni azienda (problema N+1), come già fatto in {@code PostRepository}.</p>
 */
public interface AziendaRepository extends JpaRepository<Azienda, Long> {

    @Override
    @EntityGraph(attributePaths = "proprietario")
    Page<Azienda> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "proprietario")
    Page<Azienda> findByProprietarioId(Long proprietarioId, Pageable pageable);

    /** Ricerca per nome (contiene, senza distinzione maiuscole/minuscole): usata dal tipo-mentre-scrivi. */
    @EntityGraph(attributePaths = "proprietario")
    Page<Azienda> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
