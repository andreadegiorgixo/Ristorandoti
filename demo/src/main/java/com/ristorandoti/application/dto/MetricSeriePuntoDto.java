package com.ristorandoti.application.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un punto della serie temporale di una metrica: valore in un dato giorno. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricSeriePuntoDto {

    private LocalDate giorno;

    private long valore;
}
