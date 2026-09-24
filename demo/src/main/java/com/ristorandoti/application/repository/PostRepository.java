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
 *
 * <p>Il feed globale e il profilo personale mostrano solo i post personali ({@code azienda IS NULL}):
 * i post pubblicati come pagina aziendale vivono solo nella home dell'azienda
 * (vedi {@link #findByAziendaId}).</p>
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * @param pageable pagina e ordinamento richiesti
     * @return una pagina del feed globale (solo post personali)
     */
    @EntityGraph(attributePaths = "autore")
    Page<Post> findByAziendaIdIsNull(Pageable pageable);

    /**
     * @param autoreId id dell'autore
     * @param pageable pagina e ordinamento richiesti
     * @return una pagina dei post personali di quell'autore
     */
    @EntityGraph(attributePaths = "autore")
    Page<Post> findByAutoreIdAndAziendaIdIsNull(Long autoreId, Pageable pageable);

    /**
     * @param aziendaId id dell'azienda
     * @param pageable  pagina e ordinamento richiesti
     * @return una pagina dei post pubblicati come pagina di quell'azienda
     */
    @EntityGraph(attributePaths = {"autore", "azienda"})
    Page<Post> findByAziendaId(Long aziendaId, Pageable pageable);
}
