package com.ristorandoti.application.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.AziendaDto;
import com.ristorandoti.application.dto.AziendaRequestDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.User;

import static com.ristorandoti.application.mapper.ProfileMapper.blankToNull;

/**
 * Mapper tra l'entità {@link Azienda} e i relativi DTO. Scritto a mano per lo stesso motivo
 * di {@link UserMapper}.
 */
@Component
public class AziendaMapper {

    /**
     * @param request     dati dell'azienda già validati
     * @param proprietario utente autenticato che la crea
     * @return una nuova entità {@link Azienda}, non ancora salvata
     */
    public Azienda toEntity(AziendaRequestDto request, User proprietario) {
        Azienda azienda = Azienda.builder()
                .proprietario(proprietario)
                .nome(request.getNome().trim())
                .tipo(request.getTipo())
                .descrizione(blankToNull(request.getDescrizione()))
                .indirizzo(blankToNull(request.getIndirizzo()))
                .citta(blankToNull(request.getCitta()))
                .telefono(blankToNull(request.getTelefono()))
                .email(blankToNull(request.getEmail()))
                .sitoWebUrl(blankToNull(request.getSitoWebUrl()))
                .fotoProfiloUrl(request.getFotoProfiloUrl().trim())
                .bannerUrl(request.getBannerUrl().trim())
                .fasciaPrezzo(request.getFasciaPrezzo())
                .build();
        azienda.replaceServizi(cleanServizi(request.getServizi()));
        return azienda;
    }

    /**
     * Applica i nuovi valori a un'azienda esistente (sostituzione completa dei campi modificabili).
     * Il proprietario non viene toccato.
     *
     * @param azienda entità da aggiornare, già gestita da JPA
     * @param request nuovi valori, già validati
     */
    public void updateEntity(Azienda azienda, AziendaRequestDto request) {
        azienda.setNome(request.getNome().trim());
        azienda.setTipo(request.getTipo());
        azienda.setDescrizione(blankToNull(request.getDescrizione()));
        azienda.setIndirizzo(blankToNull(request.getIndirizzo()));
        azienda.setCitta(blankToNull(request.getCitta()));
        azienda.setTelefono(blankToNull(request.getTelefono()));
        azienda.setEmail(blankToNull(request.getEmail()));
        azienda.setSitoWebUrl(blankToNull(request.getSitoWebUrl()));
        azienda.setFotoProfiloUrl(request.getFotoProfiloUrl().trim());
        azienda.setBannerUrl(request.getBannerUrl().trim());
        azienda.setFasciaPrezzo(request.getFasciaPrezzo());
        azienda.replaceServizi(cleanServizi(request.getServizi()));
    }

    /**
     * @param azienda azienda con proprietario già caricato
     * @return il DTO da restituire al client
     */
    public AziendaDto toDto(Azienda azienda) {
        return AziendaDto.builder()
                .id(azienda.getId())
                .proprietarioId(azienda.getProprietario().getId())
                .proprietarioName(azienda.getProprietario().getName())
                .nome(azienda.getNome())
                .tipo(azienda.getTipo())
                .descrizione(azienda.getDescrizione())
                .indirizzo(azienda.getIndirizzo())
                .citta(azienda.getCitta())
                .telefono(azienda.getTelefono())
                .email(azienda.getEmail())
                .sitoWebUrl(azienda.getSitoWebUrl())
                .fotoProfiloUrl(azienda.getFotoProfiloUrl())
                .bannerUrl(azienda.getBannerUrl())
                .fasciaPrezzo(azienda.getFasciaPrezzo())
                // Copia in una lista semplice: quella di Hibernate è lazy e la sessione è già
                // chiusa quando Jackson serializza la risposta (fuori dalla transazione).
                .servizi(new ArrayList<>(azienda.getServizi()))
                .dataCreazione(azienda.getDataCreazione())
                .build();
    }

    /**
     * Come {@link #toDto(Azienda)}, arricchito con le statistiche di follow e con {@code gestibileDaMe}.
     * Usato solo dalla lettura della singola azienda ({@code GET /api/aziende/{id}}): le liste
     * (elenco generale, elenco per proprietario, ricerca) non ne hanno bisogno.
     *
     * @param azienda        azienda con proprietario già caricato
     * @param followersCount numero di follower della pagina aziendale
     * @param followedByMe   se l'utente che fa la richiesta segue questa pagina
     * @param gestibileDaMe  se l'utente che fa la richiesta è proprietario o persona autorizzata
     * @return il DTO da restituire al client
     */
    public AziendaDto toDto(Azienda azienda, long followersCount, boolean followedByMe, boolean gestibileDaMe) {
        AziendaDto dto = toDto(azienda);
        dto.setFollowersCount(followersCount);
        dto.setFollowedByMe(followedByMe);
        dto.setGestibileDaMe(gestibileDaMe);
        return dto;
    }

    /**
     * @param servizi servizi ricevuti dal client, eventualmente {@code null}
     * @return i servizi puliti (senza spazi ai bordi né voci vuote), mai {@code null}
     */
    private List<String> cleanServizi(List<String> servizi) {
        if (servizi == null) {
            return new ArrayList<>();
        }
        List<String> puliti = new ArrayList<>();
        for (String servizio : servizi) {
            String trimmed = servizio.trim();
            if (!trimmed.isEmpty()) {
                puliti.add(trimmed);
            }
        }
        return puliti;
    }
}
