package com.ristorandoti.application.exception;

/**
 * Lanciata quando il login ({@code POST /api/auth/login}) fallisce per email inesistente
 * o password errata. Intercettata da {@link GlobalExceptionHandler}, che la traduce in
 * una risposta {@code 401 Unauthorized}.
 *
 * <p>Nome scelto volutamente diverso da
 * {@link org.springframework.security.authentication.BadCredentialsException} (già esistente
 * in Spring Security) per evitare ambiguità negli import. Il messaggio è sempre generico
 * ("email o password non corretti") per non rivelare a un attaccante se l'email esiste.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
