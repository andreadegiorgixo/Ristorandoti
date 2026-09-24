package com.ristorandoti.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.ProfileDto;
import com.ristorandoti.application.dto.ProfileUpdateRequestDto;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.ProfileMapper;
import com.ristorandoti.application.repository.FollowRepository;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.ristorandoti.application.mapper.ProfileMapper.blankToNull;

/**
 * Logica di business del profilo professionale: lettura (proprio profilo o di un altro utente)
 * e aggiornamento del proprio profilo.
 *
 * <p>Tutti i metodi pubblici sono transazionali: con {@code open-in-view=false} le collezioni lazy
 * (esperienze, istruzione) devono essere lette qui, prima di restituire il DTO al controller.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;
    private final ProfileMapper profileMapper;

    /**
     * Crea il profilo vuoto di un utente appena registrato. Invocato da {@link AuthService}
     * nella stessa transazione della registrazione.
     *
     * @param user utente appena salvato (con id valorizzato)
     */
    @Transactional
    public void createEmptyProfile(User user) {
        profileRepository.save(Profile.builder().user(user).build());
    }

    /**
     * Restituisce il profilo completo di un utente, comprese le statistiche di follow.
     *
     * @param userId        id dell'utente di cui leggere il profilo
     * @param currentUserId id dell'utente che fa la richiesta (per {@code followedByMe})
     * @return profilo con esperienze, istruzione e statistiche di follow
     * @throws ResourceNotFoundException se l'utente (e quindi il profilo) non esiste
     */
    @Transactional(readOnly = true)
    public ProfileDto getProfile(Long userId, Long currentUserId) {
        Profile profile = findByUserId(userId);
        long followersCount = followRepository.countByFollowedId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        boolean followedByMe = !currentUserId.equals(userId)
                && followRepository.existsByFollowerIdAndFollowedId(currentUserId, userId);
        long recensioniCount = reviewRepository.countByDestinatarioId(userId);
        Double valutazioneMedia = reviewRepository.averageValutazioneByDestinatarioId(userId);
        return profileMapper.toDto(profile, followersCount, followingCount, followedByMe,
                recensioniCount, valutazioneMedia);
    }

    /**
     * Aggiorna il profilo dell'utente autenticato. Regole di merge descritte in
     * {@link ProfileUpdateRequestDto}: {@code null} = invariato, {@code ""} = svuota,
     * liste valorizzate = sostituzione completa.
     *
     * @param userId  id dell'utente autenticato (dal JWT, mai dal body)
     * @param request modifiche già validate
     * @return il profilo aggiornato
     * @throws ResourceNotFoundException se il profilo non esiste
     */
    @Transactional
    public ProfileDto updateMyProfile(Long userId, ProfileUpdateRequestDto request) {
        Profile profile = findByUserId(userId);

        if (request.getProfilePictureUrl() != null) {
            profile.setProfilePictureUrl(blankToNull(request.getProfilePictureUrl()));
        }
        if (request.getBannerUrl() != null) {
            profile.setBannerUrl(blankToNull(request.getBannerUrl()));
        }
        if (request.getSommario() != null) {
            profile.setSommario(blankToNull(request.getSommario()));
        }
        if (request.getEsperienze() != null) {
            profile.replaceEsperienze(profileMapper.toExperiences(request.getEsperienze()));
        }
        if (request.getIstruzione() != null) {
            profile.replaceIstruzione(profileMapper.toEducations(request.getIstruzione()));
        }

        // flush: le nuove esperienze/istruzione ricevono l'id prima di essere mappate nel DTO
        Profile saved = profileRepository.saveAndFlush(profile);
        log.debug("Profilo {} aggiornato dall'utente {}", saved.getId(), userId);
        long followersCount = followRepository.countByFollowedId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long recensioniCount = reviewRepository.countByDestinatarioId(userId);
        Double valutazioneMedia = reviewRepository.averageValutazioneByDestinatarioId(userId);
        return profileMapper.toDto(saved, followersCount, followingCount, false, recensioniCount, valutazioneMedia);
    }

    private Profile findByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profilo non trovato per l'utente " + userId));
    }
}
