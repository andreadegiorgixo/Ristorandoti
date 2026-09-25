package com.ristorandoti.application.dto;

import com.ristorandoti.application.entity.AziendaRuoloCodice;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO per il body di {@code POST /api/aziende/{aziendaId}/dashboard/permessi/{userId}}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssegnaRuoloRequestDto {

    @NotNull(message = "Il ruolo è obbligatorio")
    private AziendaRuoloCodice ruolo;
}
