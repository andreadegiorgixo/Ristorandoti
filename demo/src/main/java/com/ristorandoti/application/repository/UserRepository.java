package com.ristorandoti.application.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.User;

/**
 * Repository Spring Data JPA per l'entità {@link User}.
 *
 * <p>Estendendo {@link JpaRepository} si ottengono già le operazioni CRUD di base
 * (save, findById, findAll, delete, ...). Qui si aggiungono solo le query derivate
 * necessarie all'autenticazione: Spring Data le implementa automaticamente leggendo
 * il nome del metodo, senza scrivere JPQL/SQL.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Cerca un utente tramite la sua email (usata come username per il login).
     *
     * @param email indirizzo email da cercare (già normalizzato in minuscolo)
     * @return un {@link Optional} con l'utente se trovato, altrimenti vuoto
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se esiste già un utente con una determinata email. Usato in registrazione per
     * rifiutare email duplicate senza caricare l'intera entità.
     *
     * @param email indirizzo email da verificare (già normalizzato in minuscolo)
     * @return {@code true} se l'email è già registrata
     */
    boolean existsByEmail(String email);
}
