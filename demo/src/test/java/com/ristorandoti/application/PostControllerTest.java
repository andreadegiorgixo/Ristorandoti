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
 * Test di integrazione della modifica di un post personale
 * ({@link com.ristorandoti.application.controller.PostController#updatePost}), stesso approccio di
 * {@link AziendaPostControllerTest}: utenti reali, JWT reale, intera catena HTTP via {@link MockMvc}.
 *
 * <p>Copre: l'autore può modificare il proprio post, un altro utente no (404, non 403: protezione
 * IDOR), e un post di pagina aziendale non è raggiungibile da questa rotta.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void modifica_autore_aggiornaIlContenuto() throws Exception {
        AuthResponseDto autore = registraUtente();
        Long postId = creaPost(autore.getToken(), "Testo originale");

        mockMvc.perform(put("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + autore.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"Testo corretto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto").value("Testo corretto"));
    }

    @Test
    void modifica_postDiUnAltroUtente_restituisce404NonEsisteEssereRivelato() throws Exception {
        AuthResponseDto autore = registraUtente();
        Long postId = creaPost(autore.getToken(), "Testo originale");

        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(put("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + estraneo.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"Testo modificato da estraneo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void modifica_postDiPaginaAziendale_nonRaggiungibileDaQuestaRotta() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Post Personale");
        Long postId = creaPostAzienda(proprietario.getToken(), aziendaId, "Post della pagina");

        mockMvc.perform(put("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"Tentativo di modifica\"}"))
                .andExpect(status().isNotFound());
    }

    private Long creaPost(String token, String contenuto) throws Exception {
        String body = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenuto\":\"%s\"}".formatted(contenuto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long creaPostAzienda(String token, Long aziendaId, String contenuto) throws Exception {
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
        String email = "post-test-" + UUID.randomUUID() + "@example.com";
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
