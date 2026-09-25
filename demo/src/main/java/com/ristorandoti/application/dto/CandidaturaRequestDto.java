package com.ristorandoti.application.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO per il body di {@code POST .../offerte-lavoro/{offertaId}/candidature}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidaturaRequestDto {

    @Size(max = 2000, message = "Il messaggio non può superare 2000 caratteri")
    private String messaggio;
}
