package com.ristorandoti.application.entity;

/** Tipo di evento registrato in {@link AziendaPermessiAuditLog}. */
public enum AzioneAuditPermessi {

    /** Un Admin ha assegnato un ruolo a un dipendente. */
    ASSEGNATO,

    /** Un Admin ha revocato esplicitamente un ruolo a un dipendente. */
    REVOCATO,

    /** Il ruolo è stato revocato d'ufficio perché il rapporto di lavoro è terminato. */
    REVOCATO_FINE_RAPPORTO
}
