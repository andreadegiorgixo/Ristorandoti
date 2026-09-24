package com.ristorandoti.application.mapper;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.OffertaLavoroDto;
import com.ristorandoti.application.dto.OffertaLavoroRequestDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.OffertaLavoro;
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
     * @param offerta offerta con azienda e autore già caricati
     * @return il DTO da restituire al client
     */
    public OffertaLavoroDto toDto(OffertaLavoro offerta) {
        return OffertaLavoroDto.builder()
                .id(offerta.getId())
                .aziendaId(offerta.getAzienda().getId())
                .aziendaNome(offerta.getAzienda().getNome())
                .autoreId(offerta.getAutore().getId())
                .autoreName(offerta.getAutore().getName())
                .titolo(offerta.getTitolo())
                .descrizione(offerta.getDescrizione())
                .dataCreazione(offerta.getDataCreazione())
                .build();
    }
}
