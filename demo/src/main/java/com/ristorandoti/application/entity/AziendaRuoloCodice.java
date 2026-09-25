package com.ristorandoti.application.entity;

/**
 * Codice dei ruoli assegnabili ai dipendenti per la gestione della pagina aziendale (tabella
 * {@code azienda_ruoli}, seed in {@code V11__azienda_capability_roles.sql}).
 *
 * <p>Il proprietario dell'azienda non riceve mai uno di questi ruoli: è Admin implicito e non
 * revocabile senza bisogno di una riga in {@code azienda_user_ruoli}.</p>
 */
public enum AziendaRuoloCodice {
    ADMIN,
    HR,
    SOCIAL_MEDIA_MANAGER
}
