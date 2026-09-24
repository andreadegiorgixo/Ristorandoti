package com.ristorandoti.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code PUT /api/reviews/{userId}}. Se l'autore ha già recensito
 * quel destinatario, i valori sostituiscono la recensione esistente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReviewRequestDto {

    @NotNull(message = "Il voto è obbligatorio")
    @Min(value = 1, message = "Il voto minimo è 1")
    @Max(value = 5, message = "Il voto massimo è 5")
    private Integer valutazione;

    @NotBlank(message = "Il testo della recensione è obbligatorio")
    @Size(max = 2000, message = "Il testo non può superare 2000 caratteri")
    private String testo;
}
