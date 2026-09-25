package com.ristorandoti.application.entity;

import java.time.LocalDate;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Riga aggregata di una {@link MetricaGiornaliera} per un'azienda in un giorno (tabella
 * {@code azienda_metric_daily}, migration Flyway {@code V15__azienda_metriche_giornaliere.sql}).
 *
 * <p>Questa entità serve solo in lettura (query del grafico della Home Dashboard, vedi
 * {@code MetricsQueryService}): le scritture avvengono con un {@code MERGE} nativo atomico
 * (vedi {@link com.ristorandoti.application.repository.AziendaMetricDailyRepository#incrementa}),
 * che bypassa Hibernate per evitare race condition sull'upsert.</p>
 */
@Entity
@Table(name = "azienda_metric_daily")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "azienda")
public class AziendaMetricDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "azienda_id", nullable = false)
    private Azienda azienda;

    @Column(name = "giorno", nullable = false)
    private LocalDate giorno;

    @Enumerated(EnumType.STRING)
    @Column(name = "metrica", nullable = false, length = 30)
    private MetricaGiornaliera metrica;

    @Column(name = "valore", nullable = false)
    private long valore;
}
