package com.ristorandoti.application.entity;

/**
 * Fascia di prezzo di un'{@link Azienda}, espressa con il simbolo "€" (da 1 a 5 simboli).
 *
 * <p>Per aggiungere un nuovo valore: aggiungere la costante qui E aggiornare il vincolo
 * {@code ck_aziende_fascia_prezzo} con una nuova migration Flyway (vedi
 * {@code V9__add_azienda_profile_fields.sql}).</p>
 */
public enum FasciaPrezzo {
    /** € — 5-25€ */
    EURO_1,
    /** €€ — 25-50€ */
    EURO_2,
    /** €€€ — 50-100€ */
    EURO_3,
    /** €€€€ — 100-200€ */
    EURO_4,
    /** €€€€€ — 200€+ */
    EURO_5
}
