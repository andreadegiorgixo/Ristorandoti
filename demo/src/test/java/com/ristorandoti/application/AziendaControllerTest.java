package com.ristorandoti.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ristorandoti.application.controller.AziendaController;
import com.ristorandoti.application.dto.AuthResponseDto;
import com.ristorandoti.application.entity.Role;
import com.ristorandoti.application.entity.RoleName;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.repository.RoleRepository;
import com.ristorandoti.application.repository.UserRepository;

/**
 * Test di integrazione di {@link AziendaController}: usa un vero utente registrato tramite
 * {@code /api/auth/register} (e non un JWT finto), così esercita l'intera catena di sicurezza
 * come farebbe un client reale.
 *
 * <p>Copre i casi richiesti per la Fase 1 (creazione con ruoli diversi, validazione fallita,
 * utente non autenticato) più il controllo di ownership su lettura singola e modifica, il punto
 * più delicato dal punto di vista della sicurezza introdotto in questa fase.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AziendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Istanza locale, non un bean Spring: nel classpath convivono Jackson 2 e Jackson 3
     * (quest'ultimo è quello autoconfigurato da Spring Boot 4 per i controller) e qui serve solo
     * per leggere le risposte JSON nei test, non per replicare la configurazione dell'app.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void creaAzienda_utenteConRuoloBase_creaConSuccesso() throws Exception {
        AuthResponseDto utente = registraUtente();

        mockMvc.perform(post("/api/aziende")
                        .header("Authorization", "Bearer " + utente.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aziendaPayload("Trattoria da Test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Trattoria da Test"))
                .andExpect(jsonPath("$.tipo").value("RISTORANTE"))
                .andExpect(jsonPath("$.proprietarioId").value(utente.getId()));
    }

    @Test
    void creaAzienda_utenteConRuoloAdmin_creaConSuccesso() throws Exception {
        AuthResponseDto registrato = registraUtente();
        String tokenAdmin = promuoviAdRuoloAdminERiloggati(registrato);

        // Stesso endpoint, stesso comportamento: la creazione non fa distinzione di ruolo.
        mockMvc.perform(post("/api/aziende")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aziendaPayload("Locale dell'Admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proprietarioId").value(registrato.getId()));
    }

    @Test
    void creaAzienda_datiNonValidi_restituisce400ConErroriDiCampo() throws Exception {
        AuthResponseDto utente = registraUtente();

        // Mancano "nome", "tipo", "fotoProfiloUrl", "bannerUrl" e "fasciaPrezzo", tutti obbligatori.
        mockMvc.perform(post("/api/aziende")
                        .header("Authorization", "Bearer " + utente.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"citta\":\"Milano\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nome").exists())
                .andExpect(jsonPath("$.fieldErrors.tipo").exists())
                .andExpect(jsonPath("$.fieldErrors.fotoProfiloUrl").exists())
                .andExpect(jsonPath("$.fieldErrors.bannerUrl").exists())
                .andExpect(jsonPath("$.fieldErrors.fasciaPrezzo").exists());
    }

    @Test
    void creaAzienda_utenteNonAutenticato_restituisce401() throws Exception {
        mockMvc.perform(post("/api/aziende")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aziendaPayload("Senza Token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_aziendaInesistente_restituisce404() throws Exception {
        AuthResponseDto utente = registraUtente();

        mockMvc.perform(get("/api/aziende/999999999")
                        .header("Authorization", "Bearer " + utente.getToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_utenteNonProprietario_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Azienda Altrui");

        AuthResponseDto altroUtente = registraUtente();

        mockMvc.perform(put("/api/aziende/" + aziendaId)
                        .header("Authorization", "Bearer " + altroUtente.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aziendaPayload("Tentativo di modifica")))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_proprietario_aggiornaConSuccesso() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Azienda Originale");

        mockMvc.perform(put("/api/aziende/" + aziendaId)
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aziendaPayload("Azienda Rinominata")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Azienda Rinominata"));
    }

    private Long creaAzienda(String token, String nome) throws Exception {
        String body = mockMvc.perform(post("/api/aziende")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aziendaPayload(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String aziendaPayload(String nome) {
        return """
                {"nome":"%s","tipo":"RISTORANTE","citta":"Milano","fotoProfiloUrl":"https://example.com/logo.png",
                "bannerUrl":"https://example.com/banner.png","fasciaPrezzo":"EURO_2","servizi":["Wifi gratuito"]}
                """.formatted(nome);
    }

    /** Registra un utente con email univoca (i test non devono collidere tra loro). */
    private AuthResponseDto registraUtente() throws Exception {
        String email = "azienda-test-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Utente Test","email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponseDto.class);
    }

    /**
     * Assegna ROLE_ADMIN all'utente registrato e rifà il login, così il nuovo JWT porta con sé
     * il ruolo aggiornato (la claim {@code roles} è calcolata al momento dell'emissione, non letta
     * dal database a ogni richiesta).
     */
    private String promuoviAdRuoloAdminERiloggati(AuthResponseDto registrato) throws Exception {
        User user = userRepository.findById(registrato.getId()).orElseThrow();
        Role ruoloAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        user.getRoles().add(ruoloAdmin);
        userRepository.save(user);

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(registrato.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponseDto.class).getToken();
    }
}
