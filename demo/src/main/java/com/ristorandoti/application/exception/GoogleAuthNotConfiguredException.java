package com.ristorandoti.application.exception;

/**
 * Lanciata quando si tenta l'accesso con Google ma la property {@code app.google.client-id}
 * non è valorizzata. Intercettata da {@link GlobalExceptionHandler}, che la traduce in
 * una risposta {@code 503 Service Unavailable}.
 */
public class GoogleAuthNotConfiguredException extends RuntimeException {

    public GoogleAuthNotConfiguredException(String message) {
        super(message);
    }
}
