package com.ristorandoti.application.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementazione di {@link UserDetailsService}: dice a Spring Security come caricare
 * un utente dal nostro database.
 *
 * <p>Non era nella lista delle classi richieste, ma è indispensabile: l'{@code AuthenticationManager}
 * (vedi {@link com.ristorandoti.application.config.AppConfig#authenticationManager}) la invoca durante
 * {@link com.ristorandoti.application.service.AuthService#login} per recuperare l'utente tramite
 * email, e poi confronta la password con l'hash BCrypt salvato.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carica un utente dato il suo "username", che in questa applicazione è l'email.
     *
     * @param email email dell'utente (già normalizzata in minuscolo dal service)
     * @return un {@link UserDetails} di Spring Security con email, hash della password e ruoli
     * @throws UsernameNotFoundException se nessun utente ha quella email. Spring Security la
     *         converte in un errore di credenziali generico, così al client non viene mai
     *         rivelato se l'email esiste o meno.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getName().name()))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
