package com.ristorandoti.application.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Infrastruttura di firma e verifica dei JWT.
 *
 * <p>Non serve una libreria JWT esterna (es. {@code jjwt}): {@code nimbus-jose-jwt} e le classi
 * {@link JwtEncoder}/{@link JwtDecoder} di Spring Security arrivano già, come dipendenze
 * transitive, dagli starter {@code security-oauth2-client} e {@code security-oauth2-resource-server}
 * presenti nel {@code pom.xml}. I token sono firmati con RSA (algoritmo RS256).</p>
 *
 * <p><b>Nota per la produzione:</b> la coppia di chiavi RSA viene generata in memoria a ogni avvio.
 * Comodo in sviluppo (zero configurazione), ma: (1) a ogni riavvio tutti i token emessi diventano
 * invalidi e gli utenti devono rifare il login; (2) con più istanze dietro un load balancer ognuna
 * avrebbe una chiave diversa. Prima di andare in produzione, sostituire {@link #rsaKeyPair()} con
 * il caricamento della chiave da un keystore o da un secret manager.</p>
 */
@Configuration
public class JwtConfig {

    /**
     * Genera la coppia di chiavi RSA a 2048 bit: la privata firma i token, la pubblica li verifica.
     *
     * @return la coppia di chiavi RSA
     */
    @Bean
    public KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossibile generare la coppia di chiavi RSA per i JWT", e);
        }
    }

    /**
     * Componente che FIRMA i JWT, usato da
     * {@link com.ristorandoti.application.security.JwtService#generateToken}.
     *
     * @param keyPair coppia di chiavi RSA
     * @return un {@link JwtEncoder} Nimbus configurato con la chiave privata
     */
    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Componente che VERIFICA i JWT in ingresso (firma e scadenza). Collegato in
     * {@link SecurityConfig} al filtro "Resource Server" di Spring Security, che per ogni
     * richiesta protetta legge l'header {@code Authorization: Bearer <token>}, lo valida con
     * questo decoder e, se valido, considera l'utente autenticato.
     *
     * @param keyPair coppia di chiavi RSA
     * @return un {@link JwtDecoder} Nimbus configurato con la sola chiave pubblica
     */
    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }
}
