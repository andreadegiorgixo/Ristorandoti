package com.ristorandoti.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean di supporto all'autenticazione, riutilizzati dal resto dell'applicazione.
 *
 * <p>Tenuti separati da {@link SecurityConfig} per distinguere la configurazione delle
 * rotte HTTP (chi può accedere a cosa) dai componenti "strumentali" (cifratura password,
 * gestore dell'autenticazione).</p>
 */
@Configuration
public class AppConfig {

    /**
     * Encoder delle password basato su BCrypt, l'algoritmo raccomandato da Spring Security:
     * usa un salt casuale per ogni hash (la stessa password produce hash diversi) ed è
     * volutamente lento, per rendere costosi gli attacchi a forza bruta.
     *
     * @return il {@link PasswordEncoder} usato per cifrare (register) e verificare (login) le password
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Espone l'{@link AuthenticationManager} che Spring Security costruisce automaticamente
     * combinando la nostra {@link com.ristorandoti.application.security.CustomUserDetailsService}
     * e il {@link PasswordEncoder} qui sopra. È usato da
     * {@link com.ristorandoti.application.service.AuthService#login} per verificare le credenziali.
     *
     * @param configuration configurazione di autenticazione fornita da Spring Security
     * @return l'{@link AuthenticationManager} pronto all'uso
     * @throws Exception se Spring Security non riesce a costruirlo
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
