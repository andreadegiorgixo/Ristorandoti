package com.ristorandoti.application.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Persona autorizzata a gestire una pagina aziendale (pubblicare post come pagina, inserire
 * offerte di lavoro) oltre al proprietario dell'{@link Azienda} (tabella
 * {@code azienda_autorizzazioni}).
 *
 * <p>Il proprietario NON ha una riga qui: è sempre autorizzato implicitamente
 * (vedi {@link com.ristorandoti.application.service.AziendaService#isManageable}).
 * Solo il proprietario può aggiungere o rimuovere persone autorizzate.</p>
 */
@Entity
@Table(name = "azienda_autorizzazioni", uniqueConstraints = @UniqueConstraint(columnNames = {"azienda_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"azienda", "user"})
public class AziendaAutorizzazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "azienda_id", nullable = false)
    private Azienda azienda;

    /** Utente autorizzato a gestire la pagina aziendale. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
