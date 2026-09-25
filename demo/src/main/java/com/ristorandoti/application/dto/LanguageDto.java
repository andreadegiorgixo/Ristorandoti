package com.ristorandoti.application.dto;

import com.ristorandoti.application.entity.LanguageLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lingua conosciuta in uscita, parte di {@link ProfileDto}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageDto {

    private Long id;

    private String lingua;

    private LanguageLevel livelloScritto;

    private LanguageLevel livelloParlato;
}
