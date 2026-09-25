package com.ristorandoti.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Test di integrazione della gestione post della Dashboard aziendale
 * ({@link com.ristorandoti.application.controller.AziendaPostController}), stesso approccio di
 * {@link AziendaControllerTest}: utenti reali, JWT reale, intera catena HTTP via {@link MockMvc}.
 *
 * <p>Copre: nascondi/mostra senza cancellare, rimozione (soft-delete), un post nascosto non
 * raggiungibile (404, non 403) da chi non ha accesso alla Dashboard nemmeno via like diretto,
 * {@code 403} per chi non ha {@code MANAGE_POSTS}, protezione IDOR cross-azienda.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AziendaPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void nascondi_proprietario_rimuoveIlPostDallaListaPubblicaMaNonDallaDashboard() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Nascondi");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Post da nascondere");

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/posts/" + postId + "/nascondi")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/azienda/" + aziendaId)
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/posts")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].visibilita").value("PRIVATO"));
    }

    @Test
    void mostra_dopoNascosto_tornaVisibilePubblicamente() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Mostra");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Post altalenante");

        nascondi(proprietario.getToken(), aziendaId, postId);

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/posts/" + postId + "/mostra")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/azienda/" + aziendaId)
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void postNascosto_nonRaggiungibileTramiteLikeDiretto_restituisce404NonEsisteEssereRivelato() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Riservata");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Contenuto riservato");
        nascondi(proprietario.getToken(), aziendaId, postId);

        AuthResponseDto estraneo = registraUtente();

        // 404, non 403: non deve nemmeno confermare che il post esiste.
        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + estraneo.getToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rimuovi_softDelete_spariceDaEntrambeLeListe() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Rimuovi");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Post da rimuovere");

        mockMvc.perform(delete("/api/aziende/" + aziendaId + "/posts/" + postId)
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/posts")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // Un secondo tentativo sullo stesso post (già rimosso) non lo ritrova più.
        mockMvc.perform(delete("/api/aziende/" + aziendaId + "/posts/" + postId)
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void nascondi_dipendenteSenzaManagePosts_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Protetta");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Post protetto");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/posts/" + postId + "/nascondi")
                        .header("Authorization", "Bearer " + estraneo.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nascondi_postDiUnAltraAzienda_restituisce404Idor() throws Exception {
        AuthResponseDto proprietarioA = registraUtente();
        Long aziendaA = creaAzienda(proprietarioA.getToken(), "Azienda A");
        Long postId = creaPost(proprietarioA.getToken(), aziendaA, "Post dell'azienda A");

        AuthResponseDto proprietarioB = registraUtente();
        Long aziendaB = creaAzienda(proprietarioB.getToken(), "Azienda B");

        // proprietarioB ha MANAGE_POSTS sulla PROPRIA azienda, ma il post appartiene ad A.
        mockMvc.perform(post("/api/aziende/" + aziendaB + "/posts/" + postId + "/nascondi")
                        .header("Authorization", "Bearer " + proprietarioB.getToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void lista_dashboard_filtroPerStato() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Filtri");
        Long pubblico = creaPost(proprietario.getToken(), aziendaId, "Pubblico");
        Long privato = creaPost(proprietario.getToken(), aziendaId, "Privato");
        nascondi(proprietario.getToken(), aziendaId, privato);

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/posts")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("stato", "PRIVATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(privato));

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/posts")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("stato", "PUBBLICO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pubblico));
    }

    @Test
    void modifica_proprietario_aggiornaIlContenuto() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Modifica");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Testo originale");

        mockMvc.perform(put("/api/aziende/" + aziendaId + "/posts/" + postId)
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"Testo corretto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto").value("Testo corretto"));
    }

    @Test
    void modifica_utenteSenzaManagePosts_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Modifica Protetta");
        Long postId = creaPost(proprietario.getToken(), aziendaId, "Testo originale");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(put("/api/aziende/" + aziendaId + "/posts/" + postId)
                        .header("Authorization", "Bearer " + estraneo.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"Testo corretto\"}"))
                .andExpect(status().isForbidden());
    }

    private void nascondi(String token, Long aziendaId, Long postId) throws Exception {
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/posts/" + postId + "/nascondi")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private Long creaPost(String token, Long aziendaId, String contenuto) throws Exception {
        String body = mockMvc.perform(post("/api/posts/azienda/" + aziendaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"%s\"}".formatted(contenuto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
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
        String email = "post-dashboard-test-" + UUID.randomUUID() + "@example.com";
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
