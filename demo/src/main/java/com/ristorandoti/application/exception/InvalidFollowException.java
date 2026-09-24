package com.ristorandoti.application.exception;

/**
 * Operazione di follow non valida (es. un utente che prova a seguire se stesso).
 * Tradotta in {@code 400 Bad Request} da {@link GlobalExceptionHandler}.
 */
public class InvalidFollowException extends RuntimeException {

    public InvalidFollowException(String message) {
        super(message);
    }
}
