package com.ristorandoti.application.entity;

/**
 * Metrica aggregata per giorno di una pagina aziendale (tabella {@code azienda_metric_daily}).
 *
 * <p>{@link #FOLLOWER_DELTA} e {@link #LIKE_DELTA} sono variazioni (+1/-1 per ogni
 * follow/unfollow o like/unlike), non totali: il valore nel tempo si ottiene sommando
 * cumulativamente le righe fino a una certa data, così un annullamento si riflette
 * naturalmente senza dover conservare i singoli timestamp di follow/unfollow o like/unlike.</p>
 */
public enum MetricaGiornaliera {

    /** Visualizzazioni uniche della pagina pubblica in un giorno (dedup per utente, gestori esclusi). */
    VISUALIZZATORI_UNICI,

    /** Variazione dei follower in un giorno (+1 per ogni follow, -1 per ogni unfollow). */
    FOLLOWER_DELTA,

    /** Variazione dei like ai post della pagina in un giorno (+1 per ogni like, -1 per ogni unlike). */
    LIKE_DELTA,

    /** Apparizioni della pagina nei risultati di ricerca del portale in un giorno (dedup per sessione+query). */
    RICERCHE
}
