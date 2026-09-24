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
import jakarta.persistence.PreUpdate;
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
 * Recensione che un utente lascia a un altro dopo aver collaborato nello stesso locale
 * (tabella {@code reviews}). La coppia autore/destinatario è UNIQUE: una seconda recensione
 * dello stesso autore verso lo stesso destinatario aggiorna quella esistente
 * (vedi {@link com.ristorandoti.application.service.ReviewService#upsert}).
 *
 * <p>Il requisito "stesso locale, periodo sovrapposto" NON è un vincolo di database (le aziende
 * sono testo libero nelle esperienze, non un'entità collegata): è verificato in
 * {@link com.ristorandoti.application.service.ReviewService#hasOverlappingExperience} prima di
 * ogni scrittura.</p>
 */
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"autore_id", "destinatario_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"autore", "destinatario"})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Utente che scrive la recensione. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autore_id", nullable = false)
    private User autore;

    /** Utente recensito. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private User destinatario;

    /** Voto da 1 a 5. */
    @Column(name = "valutazione", nullable = false)
    private int valutazione;

    @Column(name = "testo", nullable = false, length = 2000)
    private String testo;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    @Column(name = "data_aggiornamento", nullable = false)
    private Instant dataAggiornamento;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (dataCreazione == null) {
            dataCreazione = now;
        }
        dataAggiornamento = now;
    }

    @PreUpdate
    void onUpdate() {
        dataAggiornamento = Instant.now();
    }
}
