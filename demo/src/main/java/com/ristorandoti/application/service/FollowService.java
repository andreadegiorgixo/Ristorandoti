package com.ristorandoti.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.FollowStatusDto;
import com.ristorandoti.application.dto.FollowUserDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.Follow;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidFollowException;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.FollowMapper;
import com.ristorandoti.application.repository.FollowRepository;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business del "follow" tra utenti (stile LinkedIn): seguire/smettere di seguire
 * e leggere le liste di follower e following.
 *
 * <p>Per trasformare una pagina di utenti in DTO servono solo due query aggiuntive in totale
 * (profili e follow dell'utente corrente), indipendentemente dal numero di elementi: vedi
 * {@link #toDtos}, sullo stesso principio di {@code PostService#toDtos}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dataCreazione"), Sort.Order.desc("id"));

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;

    /**
     * Inizia a seguire un utente. Idempotente: un secondo follow verso lo stesso utente non ha effetto.
     *
     * @param followerId utente autenticato (dal JWT)
     * @param followedId utente da seguire
     * @return lo stato aggiornato del follow
     * @throws InvalidFollowException     se {@code followerId} e {@code followedId} coincidono
     * @throws ResourceNotFoundException se l'utente da seguire non esiste
     */
    @Transactional
    public FollowStatusDto follow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new InvalidFollowException("Non puoi seguire te stesso");
        }
        User followed = userRepository.findById(followedId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente " + followedId + " non trovato"));

        if (!followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            followRepository.save(Follow.builder()
                    .follower(userRepository.getReferenceById(followerId))
                    .followed(followed)
                    .build());
            log.debug("Utente {} ha iniziato a seguire l'utente {}", followerId, followedId);
        }
        return toStatusDto(followerId, followedId);
    }

    /**
     * Smette di seguire un utente. Idempotente: se il follow non c'era non succede nulla.
     *
     * @param followerId utente autenticato (dal JWT)
     * @param followedId utente da smettere di seguire
     * @return lo stato aggiornato del follow
     */
    @Transactional
    public FollowStatusDto unfollow(Long followerId, Long followedId) {
        followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
        followRepository.flush();
        return toStatusDto(followerId, followedId);
    }

    /**
     * @param userId        utente di cui elencare i follower
     * @param currentUserId utente che fa la richiesta (per {@code followedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina di chi segue {@code userId}, dal più recente
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<FollowUserDto> getFollowers(Long userId, Long currentUserId, int page, int size) {
        ensureUserExists(userId);
        Page<Follow> follows = followRepository.findByFollowedId(userId, pageRequest(page, size));
        List<User> users = follows.getContent().stream().map(Follow::getFollower).toList();
        return PageResponseDto.of(follows, toDtos(users, currentUserId));
    }

    /**
     * @param userId        utente di cui elencare i seguiti
     * @param currentUserId utente che fa la richiesta (per {@code followedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina di chi è seguito da {@code userId}, dal più recente
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<FollowUserDto> getFollowing(Long userId, Long currentUserId, int page, int size) {
        ensureUserExists(userId);
        Page<Follow> follows = followRepository.findByFollowerId(userId, pageRequest(page, size));
        List<User> users = follows.getContent().stream().map(Follow::getFollowed).toList();
        return PageResponseDto.of(follows, toDtos(users, currentUserId));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Utente " + userId + " non trovato");
        }
    }

    private FollowStatusDto toStatusDto(Long followerId, Long followedId) {
        return FollowStatusDto.builder()
                .followedByMe(followRepository.existsByFollowerIdAndFollowedId(followerId, followedId))
                .followersCount(followRepository.countByFollowedId(followedId))
                .build();
    }

    /**
     * Converte una lista di utenti in DTO con due query in blocco (profili, follow dell'utente corrente).
     */
    private List<FollowUserDto> toDtos(List<User> users, Long currentUserId) {
        if (users.isEmpty()) {
            return List.of(); // Oracle non accetta "IN ()"
        }

        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());

        Map<Long, Profile> profilesByUser = profileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        Set<Long> followedByMe = followRepository.findFollowedIds(currentUserId, userIds);

        return users.stream()
                .map(user -> followMapper.toDto(user, profilesByUser.get(user.getId()), followedByMe.contains(user.getId())))
                .toList();
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
