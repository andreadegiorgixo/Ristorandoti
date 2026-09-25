package com.ristorandoti.application.dto;

import java.time.Instant;
import java.util.List;

import com.ristorandoti.application.entity.FasciaPrezzo;
import com.ristorandoti.application.entity.TipoAzienda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Azienda restituita dalle API di {@code /api/aziende}. Include i dati del proprietario
 * (id e nome), così il client non deve caricare separatamente il suo profilo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaDto {

    private Long id;

    private Long proprietarioId;

    private String proprietarioName;

    private String nome;

    private TipoAzienda tipo;

    private String descrizione;

    private String indirizzo;

    private String citta;

    private String telefono;

    private String email;

    private String sitoWebUrl;

    /** URL del logo aziendale (quadrato). */
    private String fotoProfiloUrl;

    /** URL del banner (rettangolare). */
    private String bannerUrl;

    private FasciaPrezzo fasciaPrezzo;

    private List<String> servizi;

    private Instant dataCreazione;

    /** Numero di persone che seguono la pagina aziendale. Valorizzato solo da {@code GET /{id}}. */
    private long followersCount;

    /** {@code true} se l'utente che fa la richiesta segue questa pagina. Valorizzato solo da {@code GET /{id}}. */
    private boolean followedByMe;

    /**
     * {@code true} se l'utente che fa la richiesta è il proprietario o una persona autorizzata,
     * quindi può pubblicare post e offerte di lavoro come questa azienda. Valorizzato solo da {@code GET /{id}}.
     *
     * @deprecated sostituito dalle capability granulari della Dashboard (vedi
     *             {@link #puoiVedereDashboard} e {@code GET /dashboard/permessi/correnti}); mantenuto
     *             per non rompere eventuali client esistenti, ma nessun nuovo controllo lo usa.
     */
    @Deprecated
    private boolean gestibileDaMe;

    /**
     * {@code true} se l'utente che fa la richiesta è il proprietario o ha almeno una capability
     * sulla Dashboard di questa azienda: mostra il pulsante "Visualizzala come dashboard" al posto
     * di "Modifica". Valorizzato solo da {@code GET /{id}}.
     */
    private boolean puoiVedereDashboard;
}
