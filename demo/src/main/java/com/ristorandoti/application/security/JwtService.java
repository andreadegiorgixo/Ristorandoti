package com.ristorandoti.application.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.ristorandoti.application.entity.User;

/**
 * Componente che genera i JWT applicativi.
 *
 * <p>La validazione (firma e scadenza) non è qui: la esegue automaticamente il filtro
 * "Resource Server" di Spring Security configurato in
 * {@link com.ristorandoti.application.config.SecurityConfig}, usando il {@code JwtDecoder}
 * di {@link com.ristorandoti.application.config.JwtConfig}.</p>
 */
@Service
public class JwtService {

    /** Claim con l'id numerico dell'utente. */
    private static final String USER_ID_CLAIM = "uid";

    /** Firma i token (bean definito in {@link com.ristorandoti.application.config.JwtConfig}). */
    private final JwtEncoder jwtEncoder;

    /** Valore della claim {@code iss} (chi ha emesso il token). Proprietà {@code app.jwt.issuer}. */
    private final String issuer;

    /** Validità del token in minuti. Proprietà {@code app.jwt.expiration-minutes}. */
    private final long expirationMinutes;

    /**
     * @param jwtEncoder        encoder che firma i token
     * @param issuer            valore della claim {@code iss}
     * @param expirationMinutes durata del token in minuti
     */
    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.jwt.issuer:ristorandoti}") String issuer,
                      @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Genera un JWT firmato (RS256) per l'utente indicato.
     *
     * <p>Claim incluse nel token:</p>
     * <ul>
     *     <li>{@code sub}: email dell'utente (diventa il "nome" dell'utente autenticato in Spring Security);</li>
     *     <li>{@code uid}: id numerico dell'utente;</li>
     *     <li>{@code name}: nome visualizzato;</li>
     *     <li>{@code roles}: ruoli, es. {@code ["ROLE_USER"]}, letti da SecurityConfig per le autorizzazioni;</li>
     *     <li>{@code iss}, {@code iat}, {@code exp}: emittente, data di emissione e di scadenza.</li>
     * </ul>
     *
     * <p>Attenzione: il contenuto di un JWT è solo codificato (Base64), NON cifrato.
     * Non inserire mai qui dati sensibili come password.</p>
     *
     * @param user utente per cui generare il token (con id, email e ruoli valorizzati)
     * @return il JWT firmato, in formato stringa compatta
     */
    public String generateToken(User user) {
        Instant now = Instant.now();

        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(user.getEmail())
                .claim(USER_ID_CLAIM, user.getId())
                .claim("name", user.getName())
                .claim("roles", roleNames)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * @return la validità del token in secondi, restituita al client in {@code AuthResponseDto.expiresIn}
     */
    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    /**
     * Estrae l'id dell'utente da un JWT applicativo già validato da Spring Security
     * (es. ottenuto con {@code @AuthenticationPrincipal Jwt jwt} in un controller).
     *
     * @param jwt token dell'utente autenticato
     * @return l'id dell'utente (claim {@code uid})
     */
    public static Long extractUserId(Jwt jwt) {
        Number uid = jwt.getClaim(USER_ID_CLAIM);
        return uid.longValue();
    }
}
