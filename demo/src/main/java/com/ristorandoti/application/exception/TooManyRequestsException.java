package com.ristorandoti.application.exception;

/**
 * Limite di richieste superato su un endpoint di tracciamento metriche. Tradotta in
 * {@code 429 Too Many Requests} da {@link GlobalExceptionHandler}.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
