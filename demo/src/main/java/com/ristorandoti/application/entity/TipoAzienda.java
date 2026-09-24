package com.ristorandoti.application.entity;

/**
 * Tipologia di locale/attività di ristorazione associata a un'{@link Azienda}.
 *
 * <p>Per aggiungere un nuovo valore: aggiungere la costante qui E aggiornare il vincolo
 * {@code ck_aziende_tipo} con una nuova migration Flyway (vedi {@code V7__create_azienda_schema.sql}).</p>
 */
public enum TipoAzienda {
    RISTORANTE,
    PIZZERIA,
    BAR,
    HOTEL,
    CATERING,
    ALTRO
}
