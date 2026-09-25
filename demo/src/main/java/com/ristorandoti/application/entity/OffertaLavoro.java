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
 * Offerta di lavoro pubblicata da un'azienda (tabella {@code offerte_lavoro}).
 *
 * <p>Scade {@code app.dashboard.job-duration-days} giorni dopo la pubblicazione
 * ({@link #dataScadenza}, calcolata server-side). {@link #stato} è solo una cache aggiornata
 * dallo scheduler di pulizia: ogni lettura ricalcola comunque lo stato effettivo confrontando
 * {@link #dataScadenza} con l'istante corrente (vedi
 * {@link com.ristorandoti.application.mapper.OffertaLavoroMapper}), quindi non dipende
 * esclusivamente dallo scheduler. Il limite di offerte contemporanee è applicativo, controllato
 * in modo transazionale in {@link com.ristorandoti.application.service.OffertaLavoroService#create}.
 * Una chiusura manuale (proprietario/HR) o la pulizia automatica dopo la scadenza cancellano la
 * riga; le candidature collegate seguono la stessa sorte via {@code ON DELETE CASCADE}.</p>
 */
@Entity
@Table(name = "offerte_lavoro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"azienda", "autore"})
public class OffertaLavoro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "azienda_id", nullable = false)
    private Azienda azienda;

    /** Utente (proprietario o persona autorizzata) che ha inserito l'offerta. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autore_id", nullable = false)
    private User autore;

    @Column(name = "titolo", nullable = false, length = 200)
    private String titolo;

    @Column(name = "descrizione", length = 2000)
    private String descrizione;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    /** Calcolata server-side alla creazione: {@code dataCreazione + jobDurationDays}. */
    @Column(name = "data_scadenza", nullable = false)
    private Instant dataScadenza;

    /** Cache aggiornata dallo scheduler; non è l'unica fonte di verità (vedi Javadoc di classe). */
    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    @Builder.Default
    private StatoOffertaLavoro stato = StatoOffertaLavoro.ATTIVA;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
