package com.ristorandoti.application.exception;

/**
 * Lanciata quando il login ({@code POST /api/auth/login}) fallisce per password errata
 * (l'email inesistente produce invece {@link ResourceNotFoundException}, 404).
 * Intercettata da {@link GlobalExceptionHandler}, che la traduce in una risposta
 * {@code 401 Unauthorized}.
 *
 * <p>Nome scelto volutamente diverso da
 * {@link org.springframework.security.authentication.BadCredentialsException} (già esistente
 * in Spring Security) per evitare ambiguità negli import.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
