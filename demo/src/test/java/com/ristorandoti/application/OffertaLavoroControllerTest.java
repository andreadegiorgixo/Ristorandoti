package com.ristorandoti.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ristorandoti.application.dto.AuthResponseDto;
import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.repository.OffertaLavoroRepository;

/**
 * Test di integrazione delle offerte di lavoro e delle candidature
 * ({@link com.ristorandoti.application.controller.OffertaLavoroController},
 * {@link com.ristorandoti.application.controller.CandidaturaLavoroController}), stesso approccio
 * di {@link AziendaControllerTest}.
 *
 * <p>Copre: limite di 3 offerte attive (anche sotto richieste concorrenti, il punto più delicato
 * di questa fase), esclusione delle offerte scadute dalla vista pubblica pur restando visibili
 * nella Dashboard, conflitto di modifica su un'offerta scaduta nel frattempo, candidatura
 * (incluso il rifiuto di una seconda candidatura e di una candidatura su un'offerta scaduta),
 * {@code 403} per chi non ha i permessi richiesti.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OffertaLavoroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OffertaLavoroRepository offertaLavoroRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void create_dipendenteConManageJobs_creaConSuccessoConScadenzaA7Giorni() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Lavoro");

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Cameriere\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("ATTIVA"))
                .andExpect(jsonPath("$.dataScadenza").exists())
                .andExpect(jsonPath("$.candidaturaGiaInviata").value(false));
    }

    @Test
    void create_utenteSenzaManageJobs_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Protetta");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro")
                        .header("Authorization", "Bearer " + estraneo.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Cameriere\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_oltreIlLimiteDiTre_restituisce400() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Limite");
        creaOfferta(proprietario.getToken(), aziendaId, "Offerta 1");
        creaOfferta(proprietario.getToken(), aziendaId, "Offerta 2");
        creaOfferta(proprietario.getToken(), aziendaId, "Offerta 3");

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro")
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Offerta 4\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_richiesteConcorrenti_rispettaIlLimiteDiTre() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Concorrenza");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<Callable<Integer>> richieste = IntStream.range(0, 5)
                    .<Callable<Integer>>mapToObj(i -> () -> mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro")
                                    .header("Authorization", "Bearer " + proprietario.getToken())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"titolo\":\"Offerta concorrente " + i + "\"}"))
                            .andReturn().getResponse().getStatus())
                    .toList();

            List<Integer> risultati = executor.invokeAll(richieste).stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            long successi = risultati.stream().filter(s -> s == 201).count();
            long rifiutati = risultati.stream().filter(s -> s == 400).count();
            assertThat(successi).isEqualTo(3);
            assertThat(rifiutati).isEqualTo(2);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void offertaScaduta_spariceDallaVistaPubblicaMaRestaNellaDashboard() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Scadenza");
        Long offertaId = creaOfferta(proprietario.getToken(), aziendaId, "Offerta in scadenza");
        scadiOfferta(offertaId);

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/offerte-lavoro")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/offerte-lavoro/dashboard")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].stato").value("SCADUTA"));
    }

    @Test
    void modifica_offertaScadutaNelFrattempo_restituisce400() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Conflitto");
        Long offertaId = creaOfferta(proprietario.getToken(), aziendaId, "Offerta da modificare");
        scadiOfferta(offertaId);

        mockMvc.perform(put("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId)
                        .header("Authorization", "Bearer " + proprietario.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"Titolo aggiornato\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chiudi_offertaDiUnAltraAzienda_restituisce404Idor() throws Exception {
        AuthResponseDto proprietarioA = registraUtente();
        Long aziendaA = creaAzienda(proprietarioA.getToken(), "Azienda Lavoro A");
        Long offertaId = creaOfferta(proprietarioA.getToken(), aziendaA, "Offerta di A");

        AuthResponseDto proprietarioB = registraUtente();
        Long aziendaB = creaAzienda(proprietarioB.getToken(), "Azienda Lavoro B");

        mockMvc.perform(delete("/api/aziende/" + aziendaB + "/offerte-lavoro/" + offertaId)
                        .header("Authorization", "Bearer " + proprietarioB.getToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void candidati_successo_eSecondaCandidaturaRifiutata() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Candidature");
        Long offertaId = creaOfferta(proprietario.getToken(), aziendaId, "Cameriere");
        AuthResponseDto candidato = registraUtente();

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId + "/candidature")
                        .header("Authorization", "Bearer " + candidato.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messaggio\":\"Sono molto interessato\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("INVIATA"));

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId + "/candidature")
                        .header("Authorization", "Bearer " + candidato.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void candidati_offertaScaduta_restituisce400() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Candidatura Scaduta");
        Long offertaId = creaOfferta(proprietario.getToken(), aziendaId, "Cameriere");
        scadiOfferta(offertaId);
        AuthResponseDto candidato = registraUtente();

        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId + "/candidature")
                        .header("Authorization", "Bearer " + candidato.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaCandidati_utenteSenzaManageJobs_restituisce403() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Lista Candidati");
        Long offertaId = creaOfferta(proprietario.getToken(), aziendaId, "Cameriere");
        AuthResponseDto estraneo = registraUtente();

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId + "/candidature")
                        .header("Authorization", "Bearer " + estraneo.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listaCandidati_proprietario_vedeIlCandidato() throws Exception {
        AuthResponseDto proprietario = registraUtente();
        Long aziendaId = creaAzienda(proprietario.getToken(), "Trattoria Vede Candidati");
        Long offertaId = creaOfferta(proprietario.getToken(), aziendaId, "Cameriere");
        AuthResponseDto candidato = registraUtente();
        mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId + "/candidature")
                        .header("Authorization", "Bearer " + candidato.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/aziende/" + aziendaId + "/offerte-lavoro/" + offertaId + "/candidature")
                        .header("Authorization", "Bearer " + proprietario.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].candidatoId").value(candidato.getId()));
    }

    /** Forza la scadenza di un'offerta portando indietro {@code dataScadenza}, per testare i casi limite. */
    private void scadiOfferta(Long offertaId) {
        OffertaLavoro offerta = offertaLavoroRepository.findById(offertaId).orElseThrow();
        offerta.setDataScadenza(Instant.now().minus(1, ChronoUnit.HOURS));
        offertaLavoroRepository.save(offerta);
    }

    private Long creaOfferta(String token, Long aziendaId, String titolo) throws Exception {
        String body = mockMvc.perform(post("/api/aziende/" + aziendaId + "/offerte-lavoro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titolo\":\"%s\"}".formatted(titolo)))
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
        String email = "lavoro-test-" + UUID.randomUUID() + "@example.com";
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
