package com.ristorandoti.application.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Formato JSON uniforme per tutte le risposte di errore dell'API, prodotto da
 * {@link com.ristorandoti.application.exception.GlobalExceptionHandler}.
 *
 * <p>Un formato unico rende semplice per qualsiasi client (frontend, Postman, app)
 * gestire gli errori sempre allo stesso modo, e non espone mai stack trace.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // i campi null (es. fieldErrors) non compaiono nel JSON
public class ErrorResponseDto {

    /** Istante in cui si è verificato l'errore. */
    private Instant timestamp;

    /** Codice di stato HTTP (es. 401, 409). */
    private int status;

    /** Descrizione dello stato HTTP (es. "Unauthorized"). */
    private String error;

    /** Messaggio leggibile, pensato per essere mostrato all'utente. */
    private String message;

    /** Path della richiesta che ha generato l'errore (es. "/api/auth/login"). */
    private String path;

    /**
     * Errori di validazione per campo ({@code nomeCampo -> messaggio}).
     * Presente solo nelle risposte 400 dovute a payload non valido.
     */
    private Map<String, String> fieldErrors;
}
