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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Voce del log di audit delle modifiche ai permessi della pagina aziendale (tabella
 * {@code azienda_permessi_audit_log}): chi ({@code attore}), cosa ({@code ruoloCodice} +
 * {@code azione}) e quando ({@code dataEvento}), consultabile dagli Admin.
 */
@Entity
@Table(name = "azienda_permessi_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"azienda", "attore", "targetUser"})
public class AziendaPermessiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "azienda_id", nullable = false)
    private Azienda azienda;

    /** Chi ha compiuto l'azione (un Admin, oppure il dipendente stesso in caso di fine rapporto). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attore_id", nullable = false)
    private User attore;

    /** Dipendente a cui l'azione si riferisce. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "ruolo_codice", nullable = false, length = 30)
    private AziendaRuoloCodice ruoloCodice;

    @Enumerated(EnumType.STRING)
    @Column(name = "azione", nullable = false, length = 30)
    private AzioneAuditPermessi azione;

    @Column(name = "dettaglio", length = 500)
    private String dettaglio;

    @Column(name = "data_evento", nullable = false, updatable = false)
    private Instant dataEvento;

    @PrePersist
    void onCreate() {
        if (dataEvento == null) {
            dataEvento = Instant.now();
        }
    }
}
