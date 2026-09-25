package com.ristorandoti.application.dto;

/**
 * Nome pubblico di una metrica della Home Dashboard, usato nei parametri e nelle risposte
 * dell'API. Distinto da {@code MetricaGiornaliera} (nomi di storage interni: {@code FOLLOWER} e
 * {@code LIKE} qui corrispondono a {@code FOLLOWER_DELTA}/{@code LIKE_DELTA} lì, perché sono
 * cumulativi e non variazioni giornaliere).
 */
public enum MetricaDashboard {
    VISUALIZZATORI_UNICI,
    FOLLOWER,
    LIKE,
    RICERCHE
}
