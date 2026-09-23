package com.ristorandoti.application.exception;

/**
 * Lanciata quando in fase di registrazione ({@code POST /api/auth/register}) viene usata
 * un'email già presente a database. Intercettata da {@link GlobalExceptionHandler}, che la
 * traduce in una risposta {@code 409 Conflict}.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
