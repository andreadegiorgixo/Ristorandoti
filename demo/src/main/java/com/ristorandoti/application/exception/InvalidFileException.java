package com.ristorandoti.application.exception;

/**
 * File caricato non accettabile (vuoto, formato non supportato, ...).
 * Tradotta in {@code 400 Bad Request} da {@link GlobalExceptionHandler}.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
