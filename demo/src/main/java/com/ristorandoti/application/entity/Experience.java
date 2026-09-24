package com.ristorandoti.application.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Esperienza lavorativa di un {@link Profile} (tabella {@code experiences}).
 *
 * <p>Il nome dell'azienda resta sempre una stringa libera ({@link #azienda}), così l'esperienza
 * è comunque visibile anche se non corrisponde a nessuna azienda registrata. Quando corrisponde,
 * {@link #aziendaCollegata} contiene il collegamento verso l'entità {@link Azienda}.</p>
 */
@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "profile")
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Profilo a cui appartiene l'esperienza. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    /** Nome dell'azienda o del ristorante, inserito liberamente dall'utente. */
    @Column(name = "azienda", nullable = false, length = 200)
    private String azienda;

    /** Azienda registrata a cui è collegata l'esperienza; {@code null} se nessuna corrispondenza. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "azienda_id")
    private Azienda aziendaCollegata;

    /** Ruolo ricoperto, es. "Sous Chef". */
    @Column(name = "ruolo", nullable = false, length = 150)
    private String ruolo;

    @Column(name = "data_start", nullable = false)
    private LocalDate dataStart;

    /** Data di fine; {@code null} se è la posizione attuale. */
    @Column(name = "data_end")
    private LocalDate dataEnd;

    @Column(name = "descrizione", length = 2000)
    private String descrizione;
}
