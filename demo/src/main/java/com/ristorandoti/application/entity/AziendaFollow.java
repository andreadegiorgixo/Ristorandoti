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
 * Relazione di "follow" di un utente verso una pagina aziendale (tabella {@code azienda_follows}),
 * stessa idea di {@link Follow} ma verso un'{@link Azienda} invece che verso un altro utente.
 *
 * <p>Tabella separata da {@code follows} perché quella è vincolata a coppie utente-utente
 * (CHECK anti-auto-follow che non ha senso qui) e le due FK punterebbero a tabelle diverse.</p>
 */
@Entity
@Table(name = "azienda_follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "azienda_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"follower", "azienda"})
public class AziendaFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Utente che segue la pagina aziendale. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    /** Azienda seguita. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "azienda_id", nullable = false)
    private Azienda azienda;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
