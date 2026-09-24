package com.ristorandoti.application.dto;

import java.util.List;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code PUT /api/profiles/me}.
 *
 * <p>Regola di aggiornamento (vedi
 * {@link com.ristorandoti.application.service.ProfileService#updateMyProfile}):</p>
 * <ul>
 *     <li>campo {@code null} o assente → valore attuale invariato;</li>
 *     <li>stringa vuota ({@code ""}) → il campo viene svuotato (es. rimuovere il banner);</li>
 *     <li>{@code esperienze}/{@code istruzione} valorizzate → SOSTITUISCONO l'intera lista
 *         (una lista vuota {@code []} cancella tutti gli elementi).</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProfileUpdateRequestDto {

    @URL(message = "URL della foto profilo non valido")
    @Size(max = 1000, message = "L'URL della foto profilo non può superare 1000 caratteri")
    private String profilePictureUrl;

    @URL(message = "URL del banner non valido")
    @Size(max = 1000, message = "L'URL del banner non può superare 1000 caratteri")
    private String bannerUrl;

    @Size(max = 500, message = "Il sommario non può superare 500 caratteri")
    private String sommario;

    @Valid
    @Size(max = 50, message = "Puoi inserire al massimo 50 esperienze")
    private List<ExperienceRequestDto> esperienze;

    @Valid
    @Size(max = 50, message = "Puoi inserire al massimo 50 percorsi di studio")
    private List<EducationRequestDto> istruzione;
}
