package com.ristorandoti.application.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configurazione CORS (Cross-Origin Resource Sharing).
 *
 * <p>Il frontend Angular gira su un'origine diversa dal backend (es. {@code http://localhost:4200}
 * contro {@code http://localhost:8080}): senza questa configurazione il browser blocca la
 * richiesta di "preflight" ({@code OPTIONS}) e quindi ogni chiamata alle API.</p>
 *
 * <p>Le origini ammesse si leggono dalla property {@code app.cors.allowed-origins}
 * (lista separata da virgole, sovrascrivibile con la variabile d'ambiente {@code CORS_ALLOWED_ORIGINS}).
 * Il bean viene usato automaticamente da Spring Security grazie a {@code .cors(...)} in
 * {@link SecurityConfig}.</p>
 */
@Configuration
public class CorsConfig {

    /** Durata (in secondi) per cui il browser può memorizzare l'esito del preflight. */
    private static final long PREFLIGHT_MAX_AGE_SECONDS = 3600;

    /**
     * Definisce le regole CORS applicate alle rotte {@code /api/**}.
     *
     * @param allowedOrigins origini del frontend autorizzate a chiamare le API
     * @return la sorgente di configurazione CORS letta da Spring Security
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(PREFLIGHT_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
