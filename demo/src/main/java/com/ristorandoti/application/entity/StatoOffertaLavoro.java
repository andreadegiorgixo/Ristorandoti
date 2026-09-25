package com.ristorandoti.application.entity;

/**
 * Stato di un'offerta di lavoro. È una cache aggiornata dallo scheduler di pulizia
 * ({@code OffertaLavoroCleanupJob}): ogni lettura ricalcola comunque lo stato effettivo
 * confrontando {@code dataScadenza} con l'istante corrente, quindi non è mai l'unica fonte di
 * verità (vedi {@link com.ristorandoti.application.mapper.OffertaLavoroMapper}).
 */
public enum StatoOffertaLavoro {
    ATTIVA,
    SCADUTA
}
