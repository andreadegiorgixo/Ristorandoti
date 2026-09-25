package com.ristorandoti.application.dto;

import java.time.Instant;

import com.ristorandoti.application.entity.PostVisibilita;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Post restituito dalle API di {@code /api/posts}. Include i dati dell'autore utili
 * alla card del feed (nome, foto, headline), così il client non deve caricare il profilo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {

    private Long id;

    private Long autoreId;

    private String autoreName;

    private String autoreProfilePictureUrl;

    /** Headline dell'autore, es. "Executive Chef presso Ristorante Da Mario". */
    private String autoreSommario;

    private String contenuto;

    private String mediaUrl;

    /** Istante di pubblicazione in formato ISO-8601 UTC, es. {@code "2026-09-23T18:26:47Z"}. */
    private Instant dataCreazione;

    private long likeCount;

    /** {@code true} se l'utente che fa la richiesta ha messo like al post. */
    private boolean likedByMe;

    /** Sempre {@code PUBBLICO} per i post personali; rilevante solo per i post di pagina aziendale. */
    private PostVisibilita visibilita;
}
