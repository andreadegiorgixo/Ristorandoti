package com.ristorandoti.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Test di integrazione del tracciamento e della lettura delle metriche della pagina aziendale
 * ({@link com.ristorandoti.application.controller.AziendaMetricheController}), stesso approccio di
 * {@link AziendaControllerTest}: utenti reali, JWT reale, intera catena HTTP via {@link MockMvc}.
 *
 * <p>Copre: follow/unfollow riflessi nella metrica Follower (anche l'annullamento), like/unlike
 * riflessi nella metrica Like solo per post di pagina aziendale, deduplica delle visualizzazioni
 * uniche per utente/giorno, esclusione del proprietario dal conteggio visualizzazioni, apparizione
 * nei risultati di ricerca, {@code 403} senza {@code VIEW_DASHBOARD} e rate limiting sull'endpoint
 * di tracciamento.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AziendaMetricheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void followEUnfollow_siRiflettonoNellaMetricaFollower() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Follower");
        AuthResponseDto follower = registraUtente();

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/follow")
                        .header("Authorization", "Bearer " + follower.getToken()))
                .andExpect(status().isOk());

        String oggi = LocalDate.now().toString();
        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "FOLLOWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalePeriodo").value(1));

        mockMvc.perform(delete("/api/aziende/" + aziendaId + "/follow")
                        .header("Authorization", "Bearer " + follower.getToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "FOLLOWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalePeriodo").value(0));
    }

    @Test
    void likeEUnlike_suPostAziendale_siRiflettonoNellaMetricaLike() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Like");
        Long postId = creaPostAzienda(proprietario.getToken(), aziendaId, "Novità in menu!");
        AuthResponseDto fan = registraUtente();

        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + fan.getToken()))
                .andExpect(status().isOk());

        String oggi = LocalDate.now().toString();
        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalePeriodo").value(1));

        mockMvc.perform(delete("/api/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + fan.getToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalePeriodo").value(0));
    }

    @Test
    void visualizzazionePagina_dedupPerUtenteEGiorno_eProprietarioEscluso() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Vista");
        AuthResponseDto visitatore = registraUtente();

        // Il proprietario visualizza la propria pagina: non deve contare.
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/metriche/eventi/visualizzazione")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isNoContent());

        // Il visitatore la visualizza due volte lo stesso giorno: deve contare una sola volta.
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/metriche/eventi/visualizzazione")
                        .header("Authorization", "Bearer " + visitatore.getToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/metriche/eventi/visualizzazione")
                        .header("Authorization", "Bearer " + visitatore.getToken()))
                .andExpect(status().isNoContent());

        String oggi = LocalDate.now().toString();
        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "VISUALIZZATORI_UNICI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalePeriodo").value(1));
    }

    @Test
    void ricerca_registraUnApparizionePerAziendaTrovata() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        String nomeUnivoco = "Ristorante Cercabile " + UUID.randomUUID();
        Long aziendaId = creaAzienda(proprietario.getToken(), nomeUnivoco);
        AuthResponseDto cercatore = registraUtente();

        mockMvc.perform(get("/api/aziende/ricerca")
                        .header("Authorization", "Bearer " + cercatore.getToken())
                        .param("q", nomeUnivoco))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        String oggi = LocalDate.now().toString();
        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "RICERCHE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie[0].totalePeriodo").value(1));
    }

    @Test
    void metriche_utenteSenzaViewDashboard_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Riservata");
        AuthResponseDto estraneo = registraUtente();
        String oggi = LocalDate.now().toString();

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/dashboard/metriche")
                        .header("Authorization", "Bearer " + estraneo.getToken())
                        .param("dal", oggi).param("al", oggi).param("metriche", "FOLLOWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void visualizzazionePagina_oltreIlLimite_restituisce429() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Rate Limited");
        AuthResponseDto visitatore = registraUtente();

        // Il default (app.rate-limit.tracking.capacity) è 30: consuma tutto il bucket...
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/aziende/" + aziendaId + "/metriche/eventi/visualizzazione")
                            .header("Authorization", "Bearer " + visitatore.getToken()))
                    .andExpect(status().isNoContent());
        }
        // ...la richiesta successiva deve essere rifiutata.
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/metriche/eventi/visualizzazione")
                        .header("Authorization", "Bearer " + visitatore.getToken()))
                .andExpect(status().isTooManyRequests());
    }

    private Long creaPostAzienda(String tokenProprietario, Long aziendaId, String contenuto) throws Exception {
        String body = mockMvc.perform(post("/api/posts/azienda/" + aziendaId)
                        .header("Authorization", "Bearer " + tokenProprietario)
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

    /** Registra un utente con email univoca (i test non devono collidere tra loro). */
    private AuthResponseDto registraUtente() throws Exception {
        String email = "metriche-test-" + UUID.randomUUID() + "@example.com";
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
