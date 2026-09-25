package com.ristorandoti.application.entity;

/**
 * Capability di gestione della Dashboard aziendale. Ogni {@link AziendaRuolo} comprende un
 * sottoinsieme di queste capability (tabella {@code azienda_ruolo_capabilities}); il codice non
 * fa mai controlli hardcoded sul nome del ruolo, solo sulla capability richiesta (vedi
 * {@link com.ristorandoti.application.service.AziendaPermissionService#ensureCapability}).
 */
public enum Capability {

    /** Accesso in lettura all'intera Dashboard (con le sezioni non abilitate in sola lettura). */
    VIEW_DASHBOARD,

    /** Aggiungere, modificare, rimuovere e nascondere i post della pagina aziendale. */
    MANAGE_POSTS,

    /** Pubblicare, modificare e chiudere le offerte di lavoro; vedere i candidati. */
    MANAGE_JOBS,

    /** Modificare la sezione Panoramica (descrizione dell'azienda). */
    MANAGE_OVERVIEW,

    /** Assegnare/revocare ruoli ai dipendenti e consultare il log di audit dei permessi. */
    MANAGE_PERMISSIONS
}
