package com.ristorandoti.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ristorandoti.application.dto.AuthResponseDto;

/**
 * Test di integrazione della gestione permessi della Dashboard aziendale
 * ({@link com.ristorandoti.application.controller.AziendaPermessiController} +
 * {@link com.ristorandoti.application.service.AziendaPermissionService}), con lo stesso approccio
 * di {@link AziendaControllerTest}: utenti reali registrati via {@code /api/auth/register}, JWT
 * reale, intera catena HTTP esercitata tramite {@link MockMvc}.
 *
 * <p>Copre: proprietario con tutte le capability, assegnazione bloccata per chi non risulta
 * assunto o è già proprietario, {@code 403} per chi non ha {@code MANAGE_PERMISSIONS}, revoca
 * esplicita, e la revoca automatica quando il rapporto di lavoro termina (auto-guarigione a
 * lettura, il requisito di sicurezza più delicato di questa fase).</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AziendaPermessiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void permessiCorrenti_proprietario_haTutteLeCapability() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria del Proprietario");

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi/correnti")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proprietario").value(true))
                .andExpect(jsonPath("$.capabilities.length()").value(5));
    }

    @Test
    void catalogoRuoli_restituisceITreRuoliDiDefault() throws Exception {
        AuthResponseDto utente = registraUtente();

        mockMvc.perform(get("/api/aziende/1/dashboard/permessi/ruoli")
                        .header("Authorization", "Bearer " + utente.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void assegna_dipendenteNonAssunto_restituisce400() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Pizzeria Senza Staff");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + estraneo.getId())
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruolo":"HR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assegna_alProprietario_restituisce400() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Bar del Proprietario");

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + proprietario.getId())
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruolo":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assegna_dipendenteAssunto_concedeLeCapabilityDelRuolo() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Ristorante Con Staff");
        AuthResponseDto dipendente = registraUtente();
        collegaEsperienzaCorrente(dipendente.getToken(), aziendaId, "Cameriere", null);

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + dipendente.getId())
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruolo":"HR"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi/correnti")
                        .header("Authorization", "Bearer " + dipendente.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proprietario").value(false))
                .andExpect(jsonPath("$.ruoli[0]").value("HR"))
                .andExpect(jsonPath("$.capabilities", org.hamcrest.Matchers.containsInAnyOrder("VIEW_DASHBOARD", "MANAGE_JOBS")));

        // Idempotente: riassegnare lo stesso ruolo non deve fallire né duplicare nulla.
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + dipendente.getId())
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruolo":"HR"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void assegna_utenteSenzaManagePermissions_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Locale Con SMM");
        AuthResponseDto socialMediaManager = registraUtente();
        collegaEsperienzaCorrente(socialMediaManager.getToken(), aziendaId, "Social Media Manager", null);
        assegnaRuolo(proprietario.getToken(), aziendaId, socialMediaManager.getId(), "SOCIAL_MEDIA_MANAGER");

        AuthResponseDto altroDipendente = registraUtente();
        collegaEsperienzaCorrente(altroDipendente.getToken(), aziendaId, "Cameriere", null);

        // Il Social Media Manager ha MANAGE_POSTS, non MANAGE_PERMISSIONS: non può assegnare ruoli.
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + altroDipendente.getId())
                        .header("Authorization", "Bearer " + socialMediaManager.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruolo":"HR"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void dipendenti_utenteSenzaManagePermissions_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Locale Riservato");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi")
                        .header("Authorization", "Bearer " + estraneo.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void revoca_dopoAssegnazione_rimuoveLaCapability() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Ristorante Revoca");
        AuthResponseDto dipendente = registraUtente();
        collegaEsperienzaCorrente(dipendente.getToken(), aziendaId, "HR", null);
        assegnaRuolo(proprietario.getToken(), aziendaId, dipendente.getId(), "HR");

        mockMvc.perform(delete("/api/aziende/" + aziendaId + "/dashboard/permessi/" + dipendente.getId() + "/HR")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi/correnti")
                        .header("Authorization", "Bearer " + dipendente.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities.length()").value(0));
    }

    @Test
    void capability_vieneRevocataAutomaticamente_quandoIlRapportoDiLavoroTermina() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Ristorante Fine Rapporto");
        AuthResponseDto dipendente = registraUtente();
        collegaEsperienzaCorrente(dipendente.getToken(), aziendaId, "HR", null);
        assegnaRuolo(proprietario.getToken(), aziendaId, dipendente.getId(), "HR");

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi/correnti")
                        .header("Authorization", "Bearer " + dipendente.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities.length()").value(2));

        // Il dipendente stesso chiude il proprio rapporto di lavoro con l'azienda (dataEnd valorizzata):
        // nessuno ha revocato esplicitamente il ruolo HR, ma il prossimo controllo deve già negarlo.
        collegaEsperienzaCorrente(dipendente.getToken(), aziendaId, "HR", LocalDate.now());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi/correnti")
                        .header("Authorization", "Bearer " + dipendente.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities.length()").value(0))
                .andExpect(jsonPath("$.ruoli.length()").value(0));

        // Il log di audit riflette la revoca automatica.
        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/permessi/audit")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].azione").value("REVOCATO_FINE_RAPPORTO"));
    }

    private void assegnaRuolo(String tokenProprietario, Long aziendaId, Long userId, String ruolo) throws Exception {
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + userId)
                        .header("Authorization", "Bearer " + tokenProprietario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruolo\":\"%s\"}".formatted(ruolo)))
                .andExpect(status().isNoContent());
    }

    /**
     * Collega (o aggiorna) l'unica esperienza lavorativa del dipendente all'azienda indicata,
     * simulando il rapporto di assunzione (auto-dichiarato, come nel modello dati attuale).
     *
     * @param dataEnd {@code null} per un rapporto ancora in corso, valorizzata per terminarlo
     */
    private void collegaEsperienzaCorrente(String token, Long aziendaId, String ruolo, LocalDate dataEnd) throws Exception {
        String dataEndJson = dataEnd != null ? "\"" + dataEnd + "\"" : "null";
        mockMvc.perform(put("/api/profiles/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"esperienze":[{"azienda":"Azienda di test","aziendaId":%d,"ruolo":"%s",
                                "dataStart":"2023-01-01","dataEnd":%s}]}
                                """.formatted(aziendaId, ruolo, dataEndJson)))
                .andExpect(status().isOk());
    }

    private Long creaAzienda(String token, String nome) throws Exception {
        String body = mockMvc.perform(post("/api/aziende")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","tipo":"RISTORANTE","citta":"Milano",
                                "fotoProfiloUrl":"https://example.com/logo.png",
                                "bannerUrl":"https://example.com/banner.png","fasciaPrezzo":"EURO_2"}
                                """.formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /** Registra un utente con email univoca (i test non devono collidere tra loro). */
    private AuthResponseDto registraUtente() throws Exception {
        String email = "permessi-test-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Utente Test","email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponseDto.class);
    }
}
