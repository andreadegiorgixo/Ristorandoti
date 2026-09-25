package com.ristorandoti.application.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.OffertaLavoroDto;
import com.ristorandoti.application.dto.OffertaLavoroRequestDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.entity.StatoOffertaLavoro;
import com.ristorandoti.application.entity.User;

import static com.ristorandoti.application.mapper.ProfileMapper.blankToNull;

/**
 * Mapper tra l'entità {@link OffertaLavoro} e i relativi DTO. Scritto a mano per lo stesso
 * motivo di {@link UserMapper}.
 */
@Component
public class OffertaLavoroMapper {

    /**
     * @param request dati dell'offerta già validati
     * @param azienda azienda per cui viene pubblicata
     * @param autore  utente autenticato che la pubblica
     * @return una nuova entità {@link OffertaLavoro}, non ancora salvata
     */
    public OffertaLavoro toEntity(OffertaLavoroRequestDto request, Azienda azienda, User autore) {
        return OffertaLavoro.builder()
                .azienda(azienda)
                .autore(autore)
                .titolo(request.getTitolo().trim())
                .descrizione(blankToNull(request.getDescrizione()))
                .build();
    }

    /**
     * Applica nuovi titolo/descrizione a un'offerta esistente (modifica dalla Dashboard).
     * Azienda, autore, date e stato non vengono toccati.
     *
     * @param offerta entità da aggiornare, già gestita da JPA
     * @param request nuovi valori, già validati
     */
    public void updateEntity(OffertaLavoro offerta, OffertaLavoroRequestDto request) {
        offerta.setTitolo(request.getTitolo().trim());
        offerta.setDescrizione(blankToNull(request.getDescrizione()));
    }

    /**
     * @param offerta offerta con azienda e autore già caricati
     * @return il DTO da restituire al client, con {@code candidaturaGiaInviata} a {@code false}
     *         (per contesti dove non è rilevante, es. liste amministrative senza un utente-visitatore)
     */
    public OffertaLavoroDto toDto(OffertaLavoro offerta) {
        return toDto(offerta, false);
    }

    /**
     * @param offerta               offerta con azienda e autore già caricati
     * @param candidaturaGiaInviata se l'utente che fa la richiesta si è già candidato
     * @return il DTO da restituire al client, con lo stato ricalcolato sulla scadenza effettiva
     *         (non fidandosi solo della colonna {@code stato}, aggiornata dallo scheduler)
     */
    public OffertaLavoroDto toDto(OffertaLavoro offerta, boolean candidaturaGiaInviata) {
        StatoOffertaLavoro statoEffettivo = offerta.getDataScadenza().isBefore(Instant.now())
                ? StatoOffertaLavoro.SCADUTA
                : offerta.getStato();
        return OffertaLavoroDto.builder()
                .id(offerta.getId())
                .aziendaId(offerta.getAzienda().getId())
                .aziendaNome(offerta.getAzienda().getNome())
                .autoreId(offerta.getAutore().getId())
                .autoreName(offerta.getAutore().getName())
                .titolo(offerta.getTitolo())
                .descrizione(offerta.getDescrizione())
                .dataCreazione(offerta.getDataCreazione())
                .dataScadenza(offerta.getDataScadenza())
                .stato(statoEffettivo)
                .candidaturaGiaInviata(candidaturaGiaInviata)
                .build();
    }
}
