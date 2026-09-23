package com.ristorandoti.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entità JPA che rappresenta un ruolo applicativo (es. {@code ROLE_USER}, {@code ROLE_ADMIN}).
 *
 * <p>Mappata sulla tabella {@code roles}, creata dalla migration Flyway
 * {@code V1__create_auth_schema.sql}, che inserisce anche i due ruoli di base come dato iniziale.
 * Il sistema non crea mai ruoli "al volo": {@link com.ristorandoti.application.service.AuthService}
 * si limita a leggerli tramite {@link com.ristorandoti.application.repository.RoleRepository}.</p>
 *
 * <p>{@code equals}/{@code hashCode} sono basati solo sull'id (best practice per le entità JPA:
 * evita di confrontare/caricare relazioni e resta stabile dentro le collezioni).</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class Role {

    /** Chiave primaria, generata dal database (colonna IDENTITY su Oracle). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nome del ruolo, persistito come stringa (es. "ROLE_USER") tramite {@link EnumType#STRING}
     * anziché come indice ordinale: resta leggibile a DB e non si rompe se in futuro
     * l'ordine delle costanti di {@link RoleName} dovesse cambiare.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleName name;
}
