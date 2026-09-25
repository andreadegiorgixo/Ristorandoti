package com.ristorandoti.application.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profilo completo restituito da {@code GET /api/profiles/me}, {@code GET /api/profiles/{userId}}
 * e {@code PUT /api/profiles/me}. Costruito da
 * {@link com.ristorandoti.application.mapper.ProfileMapper#toDto}.
 *
 * <p>Non contiene l'email: il profilo è visibile a tutti gli utenti autenticati.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDto {

    private Long id;

    /** Id dell'utente proprietario (da usare per {@code GET /api/posts/user/{userId}}). */
    private Long userId;

    /** Nome visualizzato dell'utente. */
    private String name;

    private String profilePictureUrl;

    private String bannerUrl;

    /** Headline professionale. */
    private String sommario;

    /** Esperienze lavorative, dalla più recente. */
    private List<ExperienceDto> esperienze;

    /** Percorsi di studio, dal più recente. */
    private List<EducationDto> istruzione;

    /** Lingue conosciute, in ordine alfabetico. */
    private List<LanguageDto> lingue;

    /** Numero di persone che seguono questo utente. */
    private long followersCount;

    /** Numero di persone seguite da questo utente. */
    private long followingCount;

    /** {@code true} se l'utente che fa la richiesta segue questo profilo (sempre {@code false} sul proprio). */
    private boolean followedByMe;

    /** Numero di recensioni ricevute. */
    private long recensioniCount;

    /** Media dei voti ricevuti (1-5), {@code null} se non ha ancora recensioni. */
    private Double valutazioneMedia;
}
