package com.ristorandoti.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code POST /api/auth/google}.
 *
 * <p>Contiene l'ID token (un JWT firmato da Google) che il frontend riceve da
 * Google Identity Services dopo che l'utente ha scelto il proprio account.
 * Viene verificato da {@link com.ristorandoti.application.security.GoogleTokenVerifier}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "idToken") // il token è una credenziale: mai nei log
public class GoogleLoginRequestDto {

    /** ID token Google (campo {@code credential} della risposta di Google Identity Services). */
    @NotBlank(message = "Il token Google è obbligatorio")
    private String idToken;
}
