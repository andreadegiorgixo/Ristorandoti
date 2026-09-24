package com.ristorandoti.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.Post;

/**
 * Repository Spring Data JPA per l'entità {@link Post}.
 *
 * <p>L'{@link EntityGraph} carica l'autore nella stessa query della pagina, evitando una query
 * in più per ogni post (problema N+1). L'ordinamento arriva dal {@link Pageable}.</p>
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * @param pageable pagina e ordinamento richiesti
     * @return una pagina del feed globale
     */
    @Override
    @EntityGraph(attributePaths = "autore")
    Page<Post> findAll(Pageable pageable);

    /**
     * @param autoreId id dell'autore
     * @param pageable pagina e ordinamento richiesti
     * @return una pagina dei post di quell'autore
     */
    @EntityGraph(attributePaths = "autore")
    Page<Post> findByAutoreId(Long autoreId, Pageable pageable);
}
