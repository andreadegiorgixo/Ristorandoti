package com.ristorandoti.application.mapper;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.ReviewDto;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.Review;

/**
 * Mapper tra l'entità {@link Review} e {@link ReviewDto}. Scritto a mano per lo stesso motivo
 * di {@link UserMapper}.
 */
@Component
public class ReviewMapper {

    /**
     * @param review        recensione con autore e destinatario già caricati
     * @param autoreProfile profilo dell'autore ({@code null} se non trovato)
     * @return il DTO della recensione
     */
    public ReviewDto toDto(Review review, Profile autoreProfile) {
        return ReviewDto.builder()
                .id(review.getId())
                .autoreId(review.getAutore().getId())
                .autoreName(review.getAutore().getName())
                .autoreProfilePictureUrl(autoreProfile != null ? autoreProfile.getProfilePictureUrl() : null)
                .autoreSommario(autoreProfile != null ? autoreProfile.getSommario() : null)
                .destinatarioId(review.getDestinatario().getId())
                .valutazione(review.getValutazione())
                .testo(review.getTesto())
                .dataCreazione(review.getDataCreazione())
                .dataAggiornamento(review.getDataAggiornamento())
                .build();
    }
}
