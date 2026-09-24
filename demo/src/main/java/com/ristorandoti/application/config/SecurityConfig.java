package com.ristorandoti.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurazione della catena di filtri di sicurezza HTTP: definisce quali rotte sono
 * pubbliche, quali richiedono autenticazione e come viene validato il JWT.
 *
 * <p>Il sistema è STATELESS: nessuna sessione lato server, nessun cookie di sessione.
 * L'identità dell'utente viaggia interamente nel JWT dell'header {@code Authorization}.
 * Per questo il CSRF è disabilitato (protegge solo l'autenticazione basata su cookie).</p>
 *
 * <p>La validazione del token è delegata al modulo "OAuth2 Resource Server" di Spring Security:
 * non serve scrivere un filtro custom ({@code OncePerRequestFilter}), basta fornirgli il
 * {@link JwtDecoder} definito in {@link JwtConfig}.</p>
 */
@Configuration
@EnableMethodSecurity // abilita @PreAuthorize sui metodi, es. @PreAuthorize("hasRole('ADMIN')")
public class SecurityConfig {

    /**
     * Rotte accessibili SENZA token. Per rendere pubblica una nuova rotta, aggiungerla qui.
     * Include gli endpoint di autenticazione, la documentazione Swagger (springdoc) e
     * l'health check dell'actuator.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/error"
    };

    /**
     * Definisce la catena di filtri applicata a ogni richiesta HTTP.
     *
     * @param http       builder di Spring Security per configurare la sicurezza HTTP
     * @param jwtDecoder decoder JWT definito in {@link JwtConfig}
     * @return la {@link SecurityFilterChain} costruita
     * @throws Exception propagata dalle API di configurazione di Spring Security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                // Usa il CorsConfigurationSource definito in CorsConfig (gestisce anche il preflight OPTIONS)
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Immagini caricate dagli utenti: visibili a tutti, caricabili solo con token
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Valida automaticamente il "Bearer token" su tutte le rotte non pubbliche:
                // token assente, scaduto o con firma non valida => 401 Unauthorized.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    /**
     * Spiega a Spring Security dove trovare i ruoli dentro il JWT.
     *
     * <p>Di default Spring cerca la claim {@code scope} e aggiunge il prefisso {@code SCOPE_}.
     * I nostri token invece (vedi {@link com.ristorandoti.application.security.JwtService}) hanno
     * una claim {@code roles} con valori già nel formato {@code ROLE_XXX}: impostiamo quindi il
     * nome della claim e un prefisso vuoto, così {@code hasRole("USER")} funziona correttamente.</p>
     *
     * @return il converter usato per trasformare le claim del JWT in authority
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
