package com.ristorandoti.application.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.AuthResponseDto;
import com.ristorandoti.application.dto.GoogleLoginRequestDto;
import com.ristorandoti.application.dto.LoginRequestDto;
import com.ristorandoti.application.dto.RegisterRequestDto;
import com.ristorandoti.application.entity.Role;
import com.ristorandoti.application.entity.RoleName;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidCredentialsException;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.exception.UserAlreadyExistsException;
import com.ristorandoti.application.mapper.UserMapper;
import com.ristorandoti.application.repository.RoleRepository;
import com.ristorandoti.application.repository.UserRepository;
import com.ristorandoti.application.security.GoogleTokenVerifier;
import com.ristorandoti.application.security.GoogleTokenVerifier.GoogleUser;
import com.ristorandoti.application.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service con la logica di business di registrazione e login.
 *
 * <p>È l'unico livello che coordina repository, sicurezza (cifratura password, autenticazione,
 * generazione JWT) e mapping: il controller resta così "sottile" (riceve la richiesta e
 * delega) e questa logica è testabile in isolamento.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Messaggio per password errata su un'email esistente. */
    private static final String INVALID_CREDENTIALS_MESSAGE = "Password non corretta";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final ProfileService profileService;

    /**
     * Registra un nuovo utente e lo autentica immediatamente restituendo un JWT
     * (così il client non deve fare una seconda chiamata di login).
     *
     * <p>Passi: normalizza l'email, verifica che non sia già usata, cifra la password con BCrypt,
     * assegna il ruolo di default {@link RoleName#ROLE_USER}, salva e genera il token.</p>
     *
     * @param request dati di registrazione già validati dal controller
     * @return token JWT e dati base dell'utente creato
     * @throws UserAlreadyExistsException se l'email è già registrata
     * @throws IllegalStateException      se il ruolo di default non esiste a DB (migration Flyway mancante)
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Esiste già un account registrato con questa email");
        }

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new HashSet<>(Set.of(defaultRole())));

        User saved = userRepository.save(user);
        profileService.createEmptyProfile(saved);
        log.info("Nuovo utente registrato con id {}", saved.getId());

        String token = jwtService.generateToken(saved);
        return userMapper.toAuthResponseDto(saved, token, jwtService.getExpirationSeconds());
    }

    /**
     * Autentica un utente tramite email e password e restituisce un nuovo JWT.
     *
     * <p>La verifica vera e propria (caricamento utente + confronto BCrypt) è fatta da
     * {@link AuthenticationManager}, che usa internamente
     * {@link com.ristorandoti.application.security.CustomUserDetailsService} e il
     * {@link PasswordEncoder}. Qualsiasi fallimento viene convertito in
     * {@link InvalidCredentialsException}.</p>
     *
     * <p>Un'email non registrata produce invece {@link ResourceNotFoundException} (404), così il
     * frontend può reindirizzare alla registrazione. Compromesso voluto: chiunque può scoprire se
     * un'email è registrata (lo rivelava già {@code /register} con il 409).</p>
     *
     * @param request credenziali già validate dal controller
     * @return token JWT e dati base dell'utente autenticato
     * @throws ResourceNotFoundException   se nessun account usa quell'email
     * @throws InvalidCredentialsException se la password non è corretta
     */
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String email = normalizeEmail(request.getEmail());

        if (!userRepository.existsByEmail(email)) {
            throw new ResourceNotFoundException("Nessun account registrato con questa email");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        String token = jwtService.generateToken(user);
        return userMapper.toAuthResponseDto(user, token, jwtService.getExpirationSeconds());
    }

    /**
     * Accesso con Google: verifica l'ID token e restituisce un JWT applicativo.
     *
     * <p>Se l'email non è ancora registrata viene creato un nuovo utente con ruolo
     * {@link RoleName#ROLE_USER}; se esiste già (anche registrato con email e password),
     * l'accesso avviene su quell'account, perché Google garantisce che l'email è verificata.</p>
     *
     * <p>Gli utenti creati tramite Google ricevono una password casuale cifrata: la colonna
     * {@code password} è obbligatoria, ma nessuno la conosce, quindi il login con password
     * per quell'account resta di fatto disabilitato.</p>
     *
     * @param request body con l'ID token Google
     * @return token JWT e dati base dell'utente
     * @throws InvalidCredentialsException se il token Google non è valido
     */
    @Transactional
    public AuthResponseDto loginWithGoogle(GoogleLoginRequestDto request) {
        GoogleUser googleUser = googleTokenVerifier.verify(request.getIdToken());
        String email = normalizeEmail(googleUser.email());

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = User.builder()
                    .email(email)
                    .name(displayName(googleUser.name(), email))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .roles(new HashSet<>(Set.of(defaultRole())))
                    .build();
            User saved = userRepository.save(created);
            profileService.createEmptyProfile(saved);
            log.info("Nuovo utente registrato con Google, id {}", saved.getId());
            return saved;
        });

        String token = jwtService.generateToken(user);
        return userMapper.toAuthResponseDto(user, token, jwtService.getExpirationSeconds());
    }

    /**
     * @return il ruolo assegnato di default ai nuovi utenti
     * @throws IllegalStateException se il ruolo non esiste a DB (migration Flyway mancante)
     */
    private Role defaultRole() {
        return roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Ruolo ROLE_USER non presente a database: verificare le migration Flyway"));
    }

    /**
     * Nome da salvare per un utente Google: quello del profilo se presente, altrimenti la parte
     * locale dell'email. Troncato a 150 caratteri come la colonna {@code users.name}.
     */
    private String displayName(String googleName, String email) {
        String name = (googleName == null || googleName.isBlank()) ? email.substring(0, email.indexOf('@')) : googleName.trim();
        return name.length() > 150 ? name.substring(0, 150) : name;
    }

    /**
     * Normalizza l'email (spazi rimossi, tutto minuscolo) così che "Mario@Mail.it" e
     * "mario@mail.it" siano considerate la stessa identità sia in registrazione che in login.
     *
     * @param email email grezza ricevuta dal client
     * @return email normalizzata
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
