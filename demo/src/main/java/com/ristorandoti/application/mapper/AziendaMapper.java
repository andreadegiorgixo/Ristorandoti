package com.ristorandoti.application.mapper;

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
        return Azienda.builder()
                .proprietario(proprietario)
                .nome(request.getNome().trim())
                .tipo(request.getTipo())
                .descrizione(blankToNull(request.getDescrizione()))
                .indirizzo(blankToNull(request.getIndirizzo()))
                .citta(blankToNull(request.getCitta()))
                .telefono(blankToNull(request.getTelefono()))
                .email(blankToNull(request.getEmail()))
                .sitoWebUrl(blankToNull(request.getSitoWebUrl()))
                .build();
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
                .dataCreazione(azienda.getDataCreazione())
                .build();
    }
}
