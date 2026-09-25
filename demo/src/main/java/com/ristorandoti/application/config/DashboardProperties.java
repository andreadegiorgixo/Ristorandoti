package com.ristorandoti.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Punto unico di configurazione per le decisioni prese dove il brief prodotto della Dashboard
 * aziendale era ambiguo (proprietà {@code app.dashboard.*} in {@code application.properties}).
 * Ogni campo corrisponde a una decisione documentata nel report finale della feature.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.dashboard")
public class DashboardProperties {

    /** Durata di un'offerta di lavoro dalla pubblicazione, in giorni. Esplicito nel brief. */
    private int jobDurationDays = 7;

    /** Giorni di conservazione di un'offerta scaduta prima della cancellazione automatica. */
    private int jobExpiredRetentionDays = 7;

    /** Numero massimo di offerte di lavoro attive contemporaneamente per azienda. */
    private int maxOfferteAttive = 3;

    /** Lunghezza massima del testo della sezione Panoramica. */
    private int overviewMaxLength = 4000;

    /**
     * Se {@code true}, la modifica dei dati anagrafici della pagina (nome, logo, copertina, ...)
     * richiede {@code MANAGE_PERMISSIONS} oltre al proprietario: non è una delle capability
     * elencate nel brief, {@code MANAGE_PERMISSIONS} (tier Admin) è la più vicina.
     */
    private boolean pageSettingsRequiresManagePermissions = true;

    /**
     * Giorni dopo i quali un post rimosso (soft-delete) viene cancellato fisicamente.
     * {@code 0} = mai (nessuna cancellazione fisica automatica in questa fase).
     */
    private int postSoftDeletePurgeDays = 0;

    /** Come viene calcolata la metrica "Ricerche". */
    private SearchMetricMode searchMetricMode = SearchMetricMode.APPEARANCE;

    /** Finestra di deduplica (in ore) per visualizzatori unici e apparizioni nei risultati di ricerca. */
    private int viewDedupTtlHours = 48;

    /**
     * Se {@code true}, la revoca di un ruolo Admin verifica che resti sempre almeno un Admin
     * attivo. Il proprietario è comunque sempre Admin implicito e non revocabile, quindi oggi
     * questo controllo è una difesa in profondità strutturalmente sempre soddisfatta; resta
     * disattivabile in vista di una futura funzione di trasferimento della proprietà.
     */
    private boolean enforceLastAdminGuard = true;

    /** Come viene calcolata la metrica "Ricerche" (vedi {@link #searchMetricMode}). */
    public enum SearchMetricMode {
        /** Conta ogni volta che la pagina compare nei risultati di una ricerca (default). */
        APPEARANCE,

        /** Riservato per il futuro: conterebbe solo i click sui risultati di ricerca. */
        CLICK
    }
}
