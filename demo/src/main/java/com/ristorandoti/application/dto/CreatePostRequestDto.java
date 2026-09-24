package com.ristorandoti.application.dto;

import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code POST /api/posts}. Serve almeno uno tra testo e foto.
 * L'autore non si indica qui: è sempre l'utente del JWT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreatePostRequestDto {

    @Size(max = 3000, message = "Il post non può superare 3000 caratteri")
    private String contenuto;

    @URL(message = "URL della foto non valido")
    @Size(max = 1000, message = "L'URL della foto non può superare 1000 caratteri")
    private String mediaUrl;

    /** Regola cross-field: un post non può essere del tutto vuoto. */
    @JsonIgnore
    @AssertTrue(message = "Il post deve contenere un testo o una foto")
    public boolean isNonVuoto() {
        return (contenuto != null && !contenuto.isBlank()) || (mediaUrl != null && !mediaUrl.isBlank());
    }
}
