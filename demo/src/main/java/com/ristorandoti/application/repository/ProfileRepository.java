package com.ristorandoti.application.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.Profile;

/**
 * Repository Spring Data JPA per l'entità {@link Profile}.
 *
 * <p>Esperienze e istruzione non hanno un repository proprio: vengono salvate e cancellate
 * in cascata tramite il profilo.</p>
 */
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * Cerca il profilo di un utente. L'{@link EntityGraph} carica l'utente nella stessa query
     * (serve il nome per la risposta); esperienze e istruzione restano lazy e vengono lette
     * dentro la transazione del service.
     *
     * @param userId id dell'utente proprietario
     * @return il profilo se esiste
     */
    @EntityGraph(attributePaths = "user")
    Optional<Profile> findByUserId(Long userId);

    /**
     * Profili di più utenti in una sola query: serve al feed per mostrare foto e headline
     * degli autori. Non chiamarlo con una collezione vuota.
     *
     * @param userIds id degli utenti
     * @return i profili trovati (senza esperienze e istruzione)
     */
    List<Profile> findByUserIdIn(Collection<Long> userIds);
}
