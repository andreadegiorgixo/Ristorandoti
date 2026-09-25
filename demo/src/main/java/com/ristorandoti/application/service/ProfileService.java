package com.ristorandoti.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.PersonaSearchResultDto;
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

    private static final int MAX_PAGE_SIZE = 50;

    private static final Sort NAME_ASC = Sort.by(Sort.Order.asc("user.name"), Sort.Order.asc("id"));

    private final ProfileRepository profileRepository;
    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;
    private final ProfileMapper profileMapper;
    private final AziendaPermissionService aziendaPermissionService;

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
        Set<Long> aziendeConRapportoAttivoPrima = request.getEsperienze() != null
                ? aziendeConRapportoAttivo(profile)
                : Set.of();

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
        if (request.getLingue() != null) {
            profile.replaceLingue(profileMapper.toLanguages(request.getLingue()));
        }

        // flush: le nuove esperienze/istruzione ricevono l'id prima di essere mappate nel DTO
        Profile saved = profileRepository.saveAndFlush(profile);
        if (request.getEsperienze() != null) {
            Set<Long> aziendeConRapportoAttivoDopo = aziendeConRapportoAttivo(saved);
            for (Long aziendaId : aziendeConRapportoAttivoPrima) {
                if (!aziendeConRapportoAttivoDopo.contains(aziendaId)) {
                    // Il rapporto di lavoro con questa azienda non risulta più "in corso" (esperienza
                    // rimossa o data_end valorizzata): chiude d'ufficio eventuali ruoli di gestione
                    // pagina attivi. Non è l'unica difesa: AziendaPermissionService.resolveCapabilities
                    // verifica comunque il rapporto ad ogni controllo, quindi l'accesso è già negato
                    // anche se questa chiamata fallisse per qualche motivo.
                    aziendaPermissionService.revokeAllRolesForEndedEmployment(aziendaId, userId);
                }
            }
        }
        log.debug("Profilo {} aggiornato dall'utente {}", saved.getId(), userId);
        long followersCount = followRepository.countByFollowedId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long recensioniCount = reviewRepository.countByDestinatarioId(userId);
        Double valutazioneMedia = reviewRepository.averageValutazioneByDestinatarioId(userId);
        return profileMapper.toDto(saved, followersCount, followingCount, false, recensioniCount, valutazioneMedia);
    }

    /**
     * Ricerca le persone il cui nome o headline contiene il testo dato (senza distinzione
     * maiuscole/minuscole), in ordine alfabetico. Usata dalla ricerca globale in navbar.
     *
     * @param query testo digitato dall'utente; se vuoto non viene eseguita nessuna ricerca
     * @param page  indice della pagina (da 0)
     * @param size  dimensione della pagina
     * @return una pagina delle persone corrispondenti, in ordine alfabetico
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PersonaSearchResultDto> search(String query, int page, int size) {
        String trimmed = query == null ? "" : query.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NAME_ASC);
        if (trimmed.isEmpty()) {
            return PageResponseDto.of(Page.empty(pageable), List.of());
        }
        Page<Profile> profiles = profileRepository.search(trimmed, pageable);
        return PageResponseDto.of(profiles, profiles.getContent().stream().map(this::toSearchResult).toList());
    }

    private PersonaSearchResultDto toSearchResult(Profile profile) {
        return PersonaSearchResultDto.builder()
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .sommario(profile.getSommario())
                .build();
    }

    private Profile findByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profilo non trovato per l'utente " + userId));
    }

    /**
     * @param profile profilo di cui esaminare le esperienze
     * @return gli id delle aziende registrate per cui il profilo ha un'esperienza ancora "in corso"
     *         ({@code dataEnd IS NULL}), cioè le aziende presso cui l'utente risulta attualmente assunto
     */
    private Set<Long> aziendeConRapportoAttivo(Profile profile) {
        return profile.getEsperienze().stream()
                .filter(e -> e.getAziendaCollegata() != null && e.getDataEnd() == null)
                .map(e -> e.getAziendaCollegata().getId())
                .collect(Collectors.toSet());
    }
}
