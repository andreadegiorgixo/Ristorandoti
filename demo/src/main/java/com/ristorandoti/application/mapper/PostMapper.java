package com.ristorandoti.application.mapper;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.CreatePostRequestDto;
import com.ristorandoti.application.dto.PostDto;
import com.ristorandoti.application.entity.Post;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;

import static com.ristorandoti.application.mapper.ProfileMapper.blankToNull;

/**
 * Mapper tra l'entità {@link Post} e i relativi DTO. Scritto a mano per lo stesso motivo
 * di {@link UserMapper}.
 */
@Component
public class PostMapper {

    /**
     * @param request dati del post già validati
     * @param autore  utente autenticato che pubblica
     * @return una nuova entità {@link Post}, non ancora salvata
     */
    public Post toEntity(CreatePostRequestDto request, User autore) {
        return Post.builder()
                .autore(autore)
                .contenuto(blankToNull(request.getContenuto()))
                .mediaUrl(blankToNull(request.getMediaUrl()))
                .build();
    }

    /**
     * Applica nuovi testo/foto a un post esistente (modifica dalla Dashboard). Autore, azienda,
     * visibilità e data di creazione non vengono toccati.
     *
     * @param post    entità da aggiornare, già gestita da JPA
     * @param request nuovi valori, già validati
     */
    public void updateEntity(Post post, CreatePostRequestDto request) {
        post.setContenuto(blankToNull(request.getContenuto()));
        post.setMediaUrl(blankToNull(request.getMediaUrl()));
    }

    /**
     * @param post           post con autore già caricato
     * @param autoreProfile  profilo dell'autore ({@code null} se non trovato)
     * @param likeCount      numero di like del post
     * @param likedByMe      se l'utente corrente ha messo like
     * @return il DTO del post
     */
    public PostDto toDto(Post post, Profile autoreProfile, long likeCount, boolean likedByMe) {
        return PostDto.builder()
                .id(post.getId())
                .autoreId(post.getAutore().getId())
                .autoreName(post.getAutore().getName())
                .autoreProfilePictureUrl(autoreProfile != null ? autoreProfile.getProfilePictureUrl() : null)
                .autoreSommario(autoreProfile != null ? autoreProfile.getSommario() : null)
                .contenuto(post.getContenuto())
                .mediaUrl(post.getMediaUrl())
                .dataCreazione(post.getDataCreazione())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .visibilita(post.getVisibilita())
                .build();
    }
}
