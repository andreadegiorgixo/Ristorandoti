package com.ristorandoti.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ristorandoti.application.dto.AuthResponseDto;
import com.ristorandoti.application.dto.GoogleLoginRequestDto;
import com.ristorandoti.application.dto.LoginRequestDto;
import com.ristorandoti.application.dto.RegisterRequestDto;
import com.ristorandoti.application.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller REST che espone gli endpoint pubblici di autenticazione.
 *
 * <p>Livello volutamente "sottile": riceve la richiesta HTTP, fa validare il body ({@link Valid}),
 * delega tutta la logica ad {@link AuthService} e restituisce la risposta con lo status corretto.
 * Gli errori non vengono gestiti qui ma da
 * {@link com.ristorandoti.application.exception.GlobalExceptionHandler}.</p>
 *
 * <p>Le rotte {@code /api/auth/**} sono dichiarate pubbliche in
 * {@link com.ristorandoti.application.config.SecurityConfig}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registra un nuovo utente.
     *
     * @param request body JSON con name, email e password
     * @return {@code 201 Created} con token JWT e dati utente
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Effettua il login di un utente esistente.
     *
     * @param request body JSON con email e password
     * @return {@code 200 OK} con token JWT e dati utente
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Accesso (o registrazione automatica) con un account Google.
     *
     * @param request body JSON con l'ID token ricevuto da Google Identity Services
     * @return {@code 200 OK} con token JWT e dati utente
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponseDto> loginWithGoogle(@Valid @RequestBody GoogleLoginRequestDto request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }
}
