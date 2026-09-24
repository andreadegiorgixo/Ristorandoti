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
 * Offerta di lavoro pubblicata da un'azienda (tabella {@code offerte_lavoro}).
 *
 * <p>Non esiste un campo "attiva": un'offerta non più disponibile viene eliminata
 * (vedi {@link com.ristorandoti.application.service.OffertaLavoroService#chiudi}), quindi
 * "offerte attive" equivale semplicemente alle righe presenti per l'azienda. Il limite di
 * 3 offerte contemporanee è applicativo, controllato in
 * {@link com.ristorandoti.application.service.OffertaLavoroService#create}.</p>
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

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
