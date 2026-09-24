package com.ristorandoti.application.exception;

/**
 * Operazione di recensione non valida (auto-recensione, oppure autore e destinatario che non
 * hanno mai lavorato nello stesso locale in un periodo sovrapposto).
 * Tradotta in {@code 400 Bad Request} da {@link GlobalExceptionHandler}.
 */
public class InvalidReviewException extends RuntimeException {

    public InvalidReviewException(String message) {
        super(message);
    }
}
