package com.ristorandoti.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * DTO (Data Transfer Object) per il body di {@code POST /api/auth/register}.
 *
 * <p>Contiene solo i dati che il client può inviare: tenerlo separato dall'entità
 * {@link com.ristorandoti.application.entity.User} impedisce al client di impostare campi
 * sensibili come {@code id} o {@code roles} (attacco "mass assignment").</p>
 *
 * <p>Le annotazioni di validazione vengono verificate automaticamente grazie a {@code @Valid}
 * nel controller ({@link com.ristorandoti.application.controller.AuthController#register});
 * se falliscono, Spring lancia {@code MethodArgumentNotValidException}, gestita da
 * {@link com.ristorandoti.application.exception.GlobalExceptionHandler} con un 400.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password") // mai loggare la password in chiaro
public class RegisterRequestDto {

    /** Nome visualizzato dell'utente. Obbligatorio, massimo 150 caratteri (come la colonna a DB). */
    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 150, message = "Il nome non può superare 150 caratteri")
    private String name;

    /** Email dell'utente, usata come username per il login. */
    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    @Size(max = 255, message = "L'email non può superare 255 caratteri")
    private String email;

    /**
     * Password in chiaro (verrà cifrata con BCrypt nel service, mai salvata così com'è).
     * Il limite massimo di 72 caratteri deriva da BCrypt, che ignora i byte oltre il 72°.
     */
    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, max = 72, message = "La password deve avere tra 8 e 72 caratteri")
    private String password;
}
