package com.ristorandoti.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.Experience;

/**
 * Repository Spring Data JPA per l'entità {@link Experience}.
 *
 * <p>Esperienze e istruzione normalmente non hanno un repository proprio (sono gestite in cascata
 * da {@code Profile}, vedi {@link ProfileRepository}): questo esiste solo per la query trasversale
 * "persone che lavorano attualmente in questa azienda", che non parte da un profilo specifico.</p>
 */
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Persone con un'esperienza collegata a questa azienda ancora in corso ({@code dataEnd IS NULL}),
     * cioè chi ci lavora attualmente. Carica anche profilo e utente per evitare N+1.
     *
     * @param aziendaId azienda di cui elencare le persone
     * @param pageable  pagina e ordinamento richiesti
     * @return una pagina delle esperienze correnti collegate all'azienda
     */
    @EntityGraph(attributePaths = {"profile", "profile.user"})
    Page<Experience> findByAziendaCollegataIdAndDataEndIsNull(Long aziendaId, Pageable pageable);
}
