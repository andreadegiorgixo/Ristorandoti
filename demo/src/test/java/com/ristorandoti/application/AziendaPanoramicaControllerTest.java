package com.ristorandoti.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Test di integrazione della sezione Panoramica ({@code PUT /api/aziende/{id}/panoramica}) e
 * della correzione di autorizzazione sui dati anagrafici della pagina (owner oppure Admin, non
 * più solo owner): stesso approccio di {@link AziendaControllerTest}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AziendaPanoramicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updatePanoramica_proprietario_aggiornaConSuccesso() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Panoramica");

        mockMvc.perform(put("/api/aziende/" + aziendaId + "/panoramica")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Una storia di famiglia dal 1950.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descrizione").value("Una storia di famiglia dal 1950."));
    }

    @Test
    void updatePanoramica_dipendenteSenzaManageOverview_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Riservata Panoramica");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(put("/api/aziende/" + aziendaId + "/panoramica")
                        .header("Authorization", "Bearer " + estraneo.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descrizione\":\"Tentativo non autorizzato\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePanoramica_oltre4000Caratteri_restituisce400() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Lunga");
        String testoTroppoLungo = "a".repeat(4001);

        mockMvc.perform(put("/api/aziende/" + aziendaId + "/panoramica")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("descrizione", testoTroppoLungo))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDatiPagina_adminNonProprietario_ammessoOraCheEDistintoDaOwnerOnly() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Admin Delegato");
        AuthResponseDto dipendente = registraUtente();

        // Collega il dipendente e promuovilo ad Admin.
        mockMvc.perform(put("/api/profiles/me")
                        .header("Authorization", "Bearer " + dipendente.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"esperienze":[{"azienda":"Trattoria Admin Delegato","aziendaId":%d,"ruolo":"Manager",
                                "dataStart":"2023-01-01"}]}
                                """.formatted(aziendaId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/dashboard/permessi/" + dipendente.getId())
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruolo\":\"ADMIN\"}"))
                .andExpect(status().isNoContent());

        // Un Admin delegato (non proprietario) può ora modificare i dati anagrafici della pagina.
        mockMvc.perform(put("/api/aziende/" + aziendaId)
                        .header("Authorization", "Bearer " + dipendente.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Nome aggiornato dall'Admin","tipo":"RISTORANTE","citta":"Milano",
                                "fotoProfiloUrl":"https://example.com/logo.png",
                                "bannerUrl":"https://example.com/banner.png","fasciaPrezzo":"EURO_2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome aggiornato dall'Admin"));
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

    private AuthResponseDto registraUtente() throws Exception {
        String email = "panoramica-test-" + UUID.randomUUID() + "@example.com";
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
