package com.ristorandoti.application.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO per il body di {@code PUT /api/aziende/{id}/panoramica}.
 *
 * <p>Il limite qui è fisso (allineato alla larghezza della colonna e al default di
 * {@code app.dashboard.overview-max-length}): le annotazioni di Bean Validation non possono
 * leggere un valore da {@code application.properties} a runtime. Se in futuro
 * {@code overview-max-length} venisse configurato più basso di 4000, questo resterebbe comunque
 * il tetto massimo assoluto (la colonna del database non può contenere di più).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanoramicaRequestDto {

    @Size(max = 4000, message = "La panoramica non può superare 4000 caratteri")
    private String descrizione;
}
