package com.ristorandoti.application.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.Role;
import com.ristorandoti.application.entity.RoleName;

/**
 * Repository Spring Data JPA per l'entità {@link Role}.
 *
 * <p>I ruoli sono dati "di sistema" inseriti dalla migration Flyway {@code V1__create_auth_schema.sql}:
 * questo repository serve quindi in lettura, per recuperare il ruolo da assegnare a un nuovo
 * utente in {@link com.ristorandoti.application.service.AuthService#register}.</p>
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Cerca un ruolo tramite il suo nome.
     *
     * @param name nome del ruolo da cercare (es. {@link RoleName#ROLE_USER})
     * @return un {@link Optional} con il ruolo se presente, altrimenti vuoto
     */
    Optional<Role> findByName(RoleName name);
}
