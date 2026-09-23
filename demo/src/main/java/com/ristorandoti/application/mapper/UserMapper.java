package com.ristorandoti.application.mapper;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.AuthResponseDto;
import com.ristorandoti.application.dto.RegisterRequestDto;
import com.ristorandoti.application.entity.Role;
import com.ristorandoti.application.entity.User;

/**
 * Mapper che converte tra l'entità {@link User} e i DTO di autenticazione.
 *
 * <p>Tenere la conversione qui (e non nel service o nel controller) significa avere un solo punto
 * da modificare quando si aggiunge un campo a un DTO o all'entità.</p>
 *
 * <p><b>Perché scritto a mano e non con MapStruct:</b> la libreria {@code mapstruct} è nel
 * {@code pom.xml}, ma il suo annotation processor ({@code mapstruct-processor}) non è configurato
 * negli {@code annotationProcessorPaths} del {@code maven-compiler-plugin}, quindi l'implementazione
 * non verrebbe mai generata. Se in futuro lo aggiungerai (insieme a {@code lombok-mapstruct-binding}),
 * questa classe potrà diventare un'interfaccia {@code @Mapper(componentModel = "spring")}.</p>
 */
@Component
public class UserMapper {

    /** Tipo di token restituito al client, secondo lo standard OAuth2 (RFC 6750). */
    private static final String TOKEN_TYPE = "Bearer";

    /**
     * Converte il DTO di registrazione in una nuova entità {@link User}.
     *
     * <p>Valorizza solo {@code name} ed {@code email}. Volutamente NON imposta:</p>
     * <ul>
     *     <li>{@code id}: generato dal database;</li>
     *     <li>{@code password}: va cifrata prima di essere assegnata (lo fa il service);</li>
     *     <li>{@code roles}: assegnati dal service (ruolo di default {@code ROLE_USER}).</li>
     * </ul>
     *
     * @param dto dati di registrazione già validati
     * @return una nuova entità {@link User}, non ancora salvata
     */
    public User toEntity(RegisterRequestDto dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }

    /**
     * Costruisce la risposta di autenticazione dall'utente e dal token appena generato.
     * Token e durata sono parametri separati (non campi dell'entità) perché esistono solo
     * nel contesto di una singola risposta HTTP.
     *
     * @param user      utente autenticato o appena registrato
     * @param token     JWT firmato
     * @param expiresIn validità del token in secondi
     * @return il DTO pronto per essere serializzato in JSON
     */
    public AuthResponseDto toAuthResponseDto(User user, String token, long expiresIn) {
        return AuthResponseDto.builder()
                .token(token)
                .tokenType(TOKEN_TYPE)
                .expiresIn(expiresIn)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(mapRoleNames(user.getRoles()))
                .build();
    }

    /**
     * Converte l'insieme di entità {@link Role} in una lista ordinata di nomi.
     *
     * @param roles ruoli dell'utente
     * @return lista dei nomi dei ruoli, es. {@code ["ROLE_USER"]}
     */
    private List<String> mapRoleNames(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
    }
}
