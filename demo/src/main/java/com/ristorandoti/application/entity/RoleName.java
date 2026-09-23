package com.ristorandoti.application.entity;

/**
 * Enum che elenca i ruoli applicativi disponibili nel sistema (RBAC - Role Based Access Control).
 *
 * <p>Viene usato sia lato JPA (persistito come {@code STRING} nella colonna {@code roles.name},
 * vedi {@link Role}) sia lato Spring Security, dove il nome dell'enum coincide esattamente con
 * il nome dell'authority (es. {@code ROLE_USER}), rispettando la convenzione di Spring Security
 * che richiede il prefisso {@code ROLE_} per i controlli basati su ruolo
 * (es. {@code hasRole("USER")} internamente cerca l'authority {@code ROLE_USER}).</p>
 *
 * <p>Per aggiungere un nuovo ruolo: aggiungere la costante qui E inserire la riga
 * corrispondente nella tabella {@code roles} tramite una nuova migration Flyway.</p>
 */
public enum RoleName {

    /** Ruolo assegnato di default a ogni nuovo utente registrato. */
    ROLE_USER,

    /** Ruolo con privilegi amministrativi, da assegnare manualmente (mai tramite la register pubblica). */
    ROLE_ADMIN
}
