package com.ristorandoti.application.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Risposta di {@code GET /api/aziende/{id}/dashboard/metriche}: una serie per ogni metrica richiesta. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricheDto {

    private List<MetricSerieDto> serie;
}
