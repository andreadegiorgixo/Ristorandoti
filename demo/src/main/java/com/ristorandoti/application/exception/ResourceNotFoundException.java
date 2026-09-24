package com.ristorandoti.application.exception;

/**
 * Lanciata quando una risorsa richiesta (utente, profilo, post, ...) non esiste.
 * Intercettata da {@link GlobalExceptionHandler}, che la traduce in una risposta
 * {@code 404 Not Found}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
