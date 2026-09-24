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
 * Percorso di studio di un {@link Profile} (tabella {@code educations}): scuola alberghiera,
 * corso, accademia di cucina, ecc.
 */
@Entity
@Table(name = "educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "profile")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Profilo a cui appartiene il percorso di studio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    /** Scuola, corso o accademia. */
    @Column(name = "istituto", nullable = false, length = 200)
    private String istituto;

    /** Titolo conseguito o in corso, es. "Diploma di Tecnico dei Servizi Enogastronomici". */
    @Column(name = "titolo_studio", nullable = false, length = 200)
    private String titoloStudio;

    @Column(name = "data_start", nullable = false)
    private LocalDate dataStart;

    /** Data di fine; {@code null} se il percorso è ancora in corso. */
    @Column(name = "data_end")
    private LocalDate dataEnd;
}
