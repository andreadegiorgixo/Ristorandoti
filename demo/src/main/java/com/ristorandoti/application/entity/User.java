package com.ristorandoti.application.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entità JPA che rappresenta un utente registrato sulla piattaforma.
 *
 * <p>Deliberatamente NON implementa {@link org.springframework.security.core.userdetails.UserDetails}:
 * l'entità di dominio resta "pulita" (solo dati di persistenza) e la conversione verso il modello
 * di Spring Security è delegata a {@link com.ristorandoti.application.security.CustomUserDetailsService},
 * mantenendo separati livello di persistenza e livello di sicurezza.</p>
 *
 * <p>Mappata sulla tabella {@code users} (vedi migration Flyway {@code V1__create_auth_schema.sql}).
 * Se aggiungi un campo qui, ricordati di aggiungere la colonna con una NUOVA migration
 * (es. {@code V2__...sql}): Hibernate è in modalità {@code validate} e l'avvio fallirebbe.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"password", "roles"}) // mai loggare la password; evita di stampare la collezione ruoli
public class User {

    /** Chiave primaria, generata dal database (colonna IDENTITY su Oracle). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Nome visualizzato dell'utente (non usato per il login). */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Email dell'utente: è anche lo "username" usato per autenticarsi
     * (vedi {@link com.ristorandoti.application.security.CustomUserDetailsService}).
     * Salvata sempre in minuscolo e vincolata UNIQUE a livello di database.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Hash BCrypt della password (mai in chiaro). La cifratura avviene in
     * {@link com.ristorandoti.application.service.AuthService#register} tramite
     * {@link org.springframework.security.crypto.password.PasswordEncoder}.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * Ruoli assegnati all'utente (relazione Many-to-Many tramite la tabella di join {@code user_roles}).
     *
     * <p>Fetch {@link FetchType#EAGER} scelto volutamente: i ruoli servono subito sia per generare
     * le claim del JWT ({@link com.ristorandoti.application.security.JwtService}) sia per costruire
     * le authority in {@link com.ristorandoti.application.security.CustomUserDetailsService}, spesso
     * fuori da una transazione: EAGER evita una {@code LazyInitializationException}. Con 1-2 ruoli
     * per utente il costo è trascurabile.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
