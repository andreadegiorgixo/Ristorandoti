package com.ristorandoti.application.exception;

/**
 * Candidatura non valida (es. già inviata per la stessa offerta, oppure offerta scaduta).
 * Tradotta in {@code 400 Bad Request} da {@link GlobalExceptionHandler}.
 */
public class InvalidCandidaturaException extends RuntimeException {

    public InvalidCandidaturaException(String message) {
        super(message);
    }
}
