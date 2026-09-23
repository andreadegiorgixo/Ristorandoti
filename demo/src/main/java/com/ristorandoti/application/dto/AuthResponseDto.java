package com.ristorandoti.application.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO restituito da {@code /api/auth/register} e {@code /api/auth/login} in caso di successo.
 *
 * <p>Contiene il JWT da usare nelle richieste successive e i dati di base dell'utente,
 * così il client può popolare subito la UI senza ulteriori chiamate.
 * Viene costruito da {@link com.ristorandoti.application.mapper.UserMapper#toAuthResponseDto}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    /** JWT firmato da inviare nell'header {@code Authorization: Bearer <token>}. */
    private String token;

    /** Tipo di token, sempre {@code "Bearer"}. */
    private String tokenType;

    /** Validità del token in secondi: dopo questo tempo il client dovrà rifare il login. */
    private long expiresIn;

    /** Id dell'utente. */
    private Long id;

    /** Nome visualizzato dell'utente. */
    private String name;

    /** Email dell'utente. */
    private String email;

    /** Ruoli dell'utente come stringhe (es. {@code ["ROLE_USER"]}). */
    private List<String> roles;
}
