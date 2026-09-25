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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Assegnazione di un {@link AziendaRuolo} a un dipendente di un'{@link Azienda} (tabella
 * {@code azienda_user_ruoli}).
 *
 * <p>Una riga con {@code dataRevoca == null} è "attiva". Una riga revocata non viene mai
 * cancellata (serve come storico per l'audit log), solo chiusa valorizzando
 * {@code dataRevoca}/{@code revocatoDa}. Il proprietario dell'azienda non ha mai una riga qui:
 * è Admin implicito e non revocabile.</p>
 */
@Entity
@Table(name = "azienda_user_ruoli")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"azienda", "user", "ruolo", "assegnatoDa", "revocatoDa"})
public class AziendaUserRuolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "azienda_id", nullable = false)
    private Azienda azienda;

    /** Dipendente a cui è assegnato il ruolo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ruolo_id", nullable = false)
    private AziendaRuolo ruolo;

    /** Chi ha assegnato il ruolo (un Admin, oppure il "sistema" in caso di fine rapporto). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assegnato_da_id", nullable = false)
    private User assegnatoDa;

    @Column(name = "data_assegnazione", nullable = false, updatable = false)
    private Instant dataAssegnazione;

    /** Chi ha revocato il ruolo; {@code null} finché la riga è attiva. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocato_da_id")
    private User revocatoDa;

    /** {@code null} finché la riga è attiva. */
    @Column(name = "data_revoca")
    private Instant dataRevoca;

    @PrePersist
    void onCreate() {
        if (dataAssegnazione == null) {
            dataAssegnazione = Instant.now();
        }
    }
}
