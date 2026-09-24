package com.ristorandoti.application.exception;

/**
 * Operazione non valida su un'offerta di lavoro (es. superamento del limite di 3 offerte attive
 * per azienda). Tradotta in {@code 400 Bad Request} da {@link GlobalExceptionHandler}.
 */
public class InvalidOffertaLavoroException extends RuntimeException {

    public InvalidOffertaLavoroException(String message) {
        super(message);
    }
}
