package com.ristorandoti.application.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Candidatura di un utente a un'{@link OffertaLavoro} (tabella {@code candidature_lavoro},
 * migration Flyway {@code V14__offerte_lavoro_scadenza_candidature.sql}). Versione minima del
 * flusso: nessuna candidatura precedente esisteva nel modello dati.
 *
 * <p>Segue la stessa retention della propria offerta: quando l'offerta viene cancellata (chiusura
 * manuale o pulizia automatica dopo la scadenza), le candidature collegate vengono cancellate a
 * cascata dal database.</p>
 */
@Entity
@Table(name = "candidature_lavoro", uniqueConstraints = @UniqueConstraint(columnNames = {"offerta_id", "candidato_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"offerta", "candidato"})
public class CandidaturaLavoro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offerta_id", nullable = false)
    private OffertaLavoro offerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidato_id", nullable = false)
    private User candidato;

    @Column(name = "messaggio", length = 2000)
    private String messaggio;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    @Builder.Default
    private StatoCandidatura stato = StatoCandidatura.INVIATA;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
