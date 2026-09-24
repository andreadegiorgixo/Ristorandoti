package com.ristorandoti.application.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ristorandoti.application.dto.ErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestore globale delle eccezioni per tutti i {@code @RestController}.
 *
 * <p>Ogni metodo annotato con {@link ExceptionHandler} intercetta un tipo di eccezione lanciata
 * da controller/service e la trasforma in una risposta JSON uniforme ({@link ErrorResponseDto})
 * con lo status HTTP appropriato. Per gestire una nuova eccezione custom basta aggiungere qui
 * un nuovo metodo {@code @ExceptionHandler}.</p>
 *
 * <p>Nota: gli errori 401 per token mancante/non valido sulle rotte protette NON passano da qui,
 * perché vengono generati dal filtro di Spring Security prima di raggiungere i controller.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Email già registrata → {@code 409 Conflict}.
     *
     * @param ex      eccezione lanciata da {@code AuthService.register}
     * @param request richiesta HTTP corrente (per valorizzare il campo {@code path})
     * @return risposta JSON di errore
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExists(UserAlreadyExistsException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /**
     * Risorsa (utente, profilo, ...) inesistente → {@code 404 Not Found}.
     * Su {@code /api/auth/login} indica un'email non registrata: il frontend può proporre la registrazione.
     *
     * @param ex      eccezione lanciata dai service
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /**
     * Violazione di un vincolo del database (es. due like simultanei dello stesso utente allo
     * stesso post, bloccati dal vincolo UNIQUE) → {@code 409 Conflict}.
     *
     * @param ex      eccezione lanciata da Spring Data
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        log.warn("Vincolo di integrità violato su {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Operazione in conflitto con lo stato attuale dei dati", request, null);
    }

    /**
     * File caricato non valido (vuoto o formato non supportato) → {@code 400 Bad Request}.
     *
     * @param ex      eccezione lanciata da {@code FileStorageService}
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidFile(InvalidFileException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /**
     * Operazione di follow non valida (es. auto-follow) → {@code 400 Bad Request}.
     *
     * @param ex      eccezione lanciata da {@code FollowService.follow}
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(InvalidFollowException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidFollow(InvalidFollowException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /**
     * File oltre {@code spring.servlet.multipart.max-file-size} → {@code 413 Payload Too Large}.
     *
     * @param ex      eccezione lanciata dal parser multipart
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "L'immagine è troppo grande: il limite è 10 MB", request, null);
    }

    /**
     * Richiesta multipart senza il campo {@code file} → {@code 400 Bad Request}.
     *
     * @param ex      eccezione lanciata da Spring MVC
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingPart(MissingServletRequestPartException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Seleziona un'immagine da caricare", request, null);
    }

    /**
     * Credenziali errate → {@code 401 Unauthorized}.
     *
     * @param ex      eccezione lanciata da {@code AuthService.login}
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    /**
     * Accesso con Google richiesto ma non configurato ({@code app.google.client-id} vuoto)
     * → {@code 503 Service Unavailable}.
     *
     * @param ex      eccezione lanciata da {@code GoogleTokenVerifier}
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(GoogleAuthNotConfiguredException.class)
    public ResponseEntity<ErrorResponseDto> handleGoogleNotConfigured(GoogleAuthNotConfiguredException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
    }

    /**
     * Qualsiasi altra eccezione di autenticazione di Spring Security non già convertita
     * → {@code 401 Unauthorized} con messaggio generico.
     *
     * @param ex      eccezione di autenticazione
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Autenticazione fallita", request, null);
    }

    /**
     * Payload che viola le regole di validazione dei DTO ({@code @NotBlank}, {@code @Email}, ...)
     * → {@code 400 Bad Request}, con il dettaglio campo per campo in {@code fieldErrors}.
     *
     * @param ex      eccezione lanciata da Spring quando fallisce un {@code @Valid}
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore con la mappa degli errori di validazione
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Dati della richiesta non validi", request, fieldErrors);
    }

    /**
     * Body JSON assente o malformato (es. virgola mancante) → {@code 400 Bad Request}.
     *
     * @param ex      eccezione lanciata dal convertitore JSON
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo della richiesta mancante o JSON non valido", request, null);
    }

    /**
     * Rotta inesistente → {@code 404 Not Found}.
     *
     * @param ex      eccezione lanciata da Spring MVC quando nessun controller gestisce il path
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NoResourceFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Risorsa non trovata", request, null);
    }

    /**
     * Metodo HTTP non supportato dalla rotta (es. GET su un endpoint solo POST) → {@code 405 Method Not Allowed}.
     *
     * @param ex      eccezione lanciata da Spring MVC
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP " + ex.getMethod() + " non supportato su questa rotta",
                request, null);
    }

    /**
     * Rete di sicurezza per ogni errore non previsto → {@code 500 Internal Server Error}.
     * Il dettaglio viene loggato lato server ma NON esposto al client (niente stack trace in risposta).
     *
     * @param ex      eccezione non gestita
     * @param request richiesta HTTP corrente
     * @return risposta JSON di errore generica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Errore non gestito su {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Si è verificato un errore interno", request, null);
    }

    /**
     * Costruisce la {@link ResponseEntity} con il formato di errore standard.
     *
     * @param status      status HTTP da restituire
     * @param message     messaggio leggibile per il client
     * @param request     richiesta HTTP corrente
     * @param fieldErrors eventuali errori di validazione per campo (può essere {@code null})
     * @return la risposta completa
     */
    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message,
                                                   HttpServletRequest request, Map<String, String> fieldErrors) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
