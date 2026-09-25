package com.ristorandoti.application.exception;

/**
 * Operazione non valida sull'assegnazione di un ruolo della Dashboard aziendale (es. destinatario
 * non attualmente assunto dall'azienda, o tentativo di assegnare un ruolo al proprietario).
 * Tradotta in {@code 400 Bad Request} da {@link GlobalExceptionHandler}.
 */
public class InvalidRoleAssignmentException extends RuntimeException {

    public InvalidRoleAssignmentException(String message) {
        super(message);
    }
}
