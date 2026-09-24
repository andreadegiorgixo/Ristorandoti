package com.ristorandoti.application.dto;

import java.util.List;

import org.hibernate.validator.constraints.URL;

import com.ristorandoti.application.entity.FasciaPrezzo;
import com.ristorandoti.application.entity.TipoAzienda;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code POST /api/aziende} e {@code PUT /api/aziende/{id}}: stessi campi
 * per creazione e modifica (sostituzione completa, non un merge parziale come
 * {@link ProfileUpdateRequestDto}). Il proprietario non si indica qui: alla creazione è sempre
 * l'utente del JWT, alla modifica non può cambiare.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AziendaRequestDto {

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 200, message = "Il nome non può superare 200 caratteri")
    private String nome;

    @NotNull(message = "Il tipo di locale è obbligatorio")
    private TipoAzienda tipo;

    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;

    @Size(max = 300, message = "L'indirizzo non può superare 300 caratteri")
    private String indirizzo;

    @Size(max = 100, message = "La città non può superare 100 caratteri")
    private String citta;

    @Size(max = 30, message = "Il telefono non può superare 30 caratteri")
    private String telefono;

    @Email(message = "Formato email non valido")
    @Size(max = 255, message = "L'email non può superare 255 caratteri")
    private String email;

    @URL(message = "URL del sito web non valido")
    @Size(max = 1000, message = "L'URL del sito web non può superare 1000 caratteri")
    private String sitoWebUrl;

    @NotBlank(message = "La foto profilo è obbligatoria")
    @Size(max = 1000, message = "L'URL della foto profilo non può superare 1000 caratteri")
    private String fotoProfiloUrl;

    @NotBlank(message = "Il banner è obbligatorio")
    @Size(max = 1000, message = "L'URL del banner non può superare 1000 caratteri")
    private String bannerUrl;

    @NotNull(message = "La fascia di prezzo è obbligatoria")
    private FasciaPrezzo fasciaPrezzo;

    @Size(max = 15, message = "Puoi aggiungere al massimo 15 servizi")
    private List<@NotBlank(message = "Il nome del servizio non può essere vuoto")
                 @Size(max = 100, message = "Il nome del servizio non può superare 100 caratteri") String> servizi;
}
