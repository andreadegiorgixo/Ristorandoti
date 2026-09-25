package com.ristorandoti.application.dto;

import com.ristorandoti.application.entity.LanguageLevel;

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
 * Lingua conosciuta in ingresso, parte di {@link ProfileUpdateRequestDto}.
 * Non ha {@code id}: la lista inviata sostituisce interamente quella salvata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LanguageRequestDto {

    @NotBlank(message = "La lingua è obbligatoria")
    @Size(max = 60, message = "La lingua non può superare 60 caratteri")
    private String lingua;

    @NotNull(message = "Il livello scritto è obbligatorio")
    private LanguageLevel livelloScritto;

    @NotNull(message = "Il livello parlato è obbligatorio")
    private LanguageLevel livelloParlato;
}
