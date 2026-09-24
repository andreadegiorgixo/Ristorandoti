package com.ristorandoti.application.mapper;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.FollowUserDto;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;

/**
 * Mapper tra {@link User} (+ {@link Profile} opzionale) e {@link FollowUserDto}. Scritto a mano
 * per lo stesso motivo di {@link UserMapper}.
 */
@Component
public class FollowMapper {

    /**
     * @param user         utente da rappresentare
     * @param profile      il suo profilo, {@code null} se non trovato
     * @param followedByMe se l'utente che fa la richiesta lo segue già
     * @return il DTO da restituire nelle liste follower/following
     */
    public FollowUserDto toDto(User user, Profile profile, boolean followedByMe) {
        return FollowUserDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .profilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .sommario(profile != null ? profile.getSommario() : null)
                .followedByMe(followedByMe)
                .build();
    }
}
