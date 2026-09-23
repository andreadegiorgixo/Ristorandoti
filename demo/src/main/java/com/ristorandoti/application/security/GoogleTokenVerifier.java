package com.ristorandoti.application.security;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.ristorandoti.application.exception.GoogleAuthNotConfiguredException;
import com.ristorandoti.application.exception.InvalidCredentialsException;

/**
 * Verifica gli ID token emessi da Google (accesso con "Sign in with Google").
 *
 * <p>Un ID token Google è un JWT firmato RS256: lo si valida come qualsiasi altro JWT,
 * scaricando le chiavi pubbliche di Google dal loro endpoint JWKS. Non serve la libreria
 * {@code google-api-client}: basta {@link NimbusJwtDecoder}, già presente tramite gli starter OAuth2.</p>
 *
 * <p>Il decoder NON è esposto come bean: {@link com.ristorandoti.application.config.SecurityConfig}
 * inietta il {@code JwtDecoder} dei token applicativi per tipo, e un secondo bean creerebbe ambiguità.</p>
 *
 * <p>Controlli eseguiti (come da documentazione Google):</p>
 * <ul>
 *     <li>firma valida con le chiavi pubbliche di Google;</li>
 *     <li>{@code iss} = {@code accounts.google.com} oppure {@code https://accounts.google.com};</li>
 *     <li>{@code aud} contiene il Client ID della nostra applicazione;</li>
 *     <li>token non scaduto ({@code exp}/{@code iat});</li>
 *     <li>{@code email_verified} = {@code true}.</li>
 * </ul>
 */
@Component
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String INVALID_TOKEN_MESSAGE = "Accesso con Google non valido o scaduto";

    /** Decoder dedicato ai token Google (null se il Client ID non è configurato). */
    private final NimbusJwtDecoder decoder;

    /**
     * @param clientId Client ID OAuth creato nella Google Cloud Console (property {@code app.google.client-id});
     *                 se vuoto l'accesso con Google resta disabilitato
     */
    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.decoder = clientId.isBlank() ? null : buildDecoder(clientId);
    }

    /**
     * Valida l'ID token e ne estrae i dati dell'utente.
     *
     * @param idToken ID token ricevuto dal frontend
     * @return email (verificata) e nome dell'utente Google
     * @throws GoogleAuthNotConfiguredException se {@code app.google.client-id} non è impostato
     * @throws InvalidCredentialsException      se il token non è valido o l'email non è verificata
     */
    public GoogleUser verify(String idToken) {
        if (decoder == null) {
            throw new GoogleAuthNotConfiguredException("L'accesso con Google non è ancora configurato sul server");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException ex) {
            throw new InvalidCredentialsException(INVALID_TOKEN_MESSAGE);
        }

        String email = jwt.getClaimAsString("email");
        // email_verified può arrivare come boolean o come stringa "true"
        boolean emailVerified = Boolean.parseBoolean(String.valueOf(jwt.getClaims().get("email_verified")));
        if (email == null || !emailVerified) {
            throw new InvalidCredentialsException("L'email dell'account Google non è verificata");
        }

        return new GoogleUser(email, jwt.getClaimAsString("name"));
    }

    private static NimbusJwtDecoder buildDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>("iss", iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString())),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(clientId))
        ));
        return decoder;
    }

    /**
     * Dati dell'utente estratti da un ID token Google valido.
     *
     * @param email email verificata dell'account Google
     * @param name  nome completo (può essere {@code null} se l'utente non lo ha condiviso)
     */
    public record GoogleUser(String email, String name) {
    }
}
