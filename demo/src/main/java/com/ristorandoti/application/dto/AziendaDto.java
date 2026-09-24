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
}
