package com.ristorandoti.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO per il body di {@code POST /api/auth/login}.
 *
 * <p>Contiene solo le credenziali: {@link com.ristorandoti.application.service.AuthService#login}
 * le passa all'{@code AuthenticationManager}, che usa
 * {@link com.ristorandoti.application.security.CustomUserDetailsService} per caricare l'utente
 * e il {@code PasswordEncoder} per confrontare la password con l'hash salvato.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class LoginRequestDto {

    /** Email dell'utente (username di login). */
    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    /** Password in chiaro, confrontata con l'hash BCrypt salvato a DB. */
    @NotBlank(message = "La password è obbligatoria")
    private String password;
}
