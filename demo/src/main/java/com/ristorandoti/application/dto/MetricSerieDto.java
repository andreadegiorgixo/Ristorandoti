package com.ristorandoti.application.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Serie temporale e statistiche di periodo di una metrica, per una card + il grafico della
 * Home Dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricSerieDto {

    private MetricaDashboard metrica;

    /** Un punto per ogni giorno del periodo richiesto (nessun buco, anche nei giorni senza eventi). */
    private List<MetricSeriePuntoDto> punti;

    /**
     * Valore della card per il periodo richiesto: somma dei valori giornalieri per le metriche
     * "di flusso" (visualizzatori, ricerche), valore cumulativo a fine periodo per le metriche
     * "di stock" (follower, like).
     */
    private long totalePeriodo;

    /**
     * Variazione percentuale rispetto al periodo precedente di uguale durata; {@code null} se non
     * calcolabile (periodo precedente a zero).
     */
    private Double variazionePercento;

    /** Primo giorno con dati disponibili per questa azienda; {@code null} se non ce ne sono ancora. */
    private LocalDate datiDisponibiliDal;
}
