package com.ristorandoti.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.FollowStatusDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.AziendaFollow;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.repository.AziendaFollowRepository;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business del "follow" di una pagina aziendale: seguire/smettere di seguire.
 * Stessa idea di {@link FollowService}, ma verso un'{@link Azienda} invece che verso un altro
 * utente: tabella e repository separati perché {@code follows} è vincolata a coppie utente-utente.
 *
 * <p>Riusa {@link FollowStatusDto}: la forma della risposta (contatore + "lo seguo già") è
 * identica a quella del follow tra utenti.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AziendaFollowService {

    private final AziendaFollowRepository aziendaFollowRepository;
    private final AziendaRepository aziendaRepository;
    private final UserRepository userRepository;

    /**
     * Inizia a seguire una pagina aziendale. Idempotente.
     *
     * @param followerId utente autenticato (dal JWT)
     * @param aziendaId  azienda da seguire
     * @return lo stato aggiornato del follow
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional
    public FollowStatusDto follow(Long followerId, Long aziendaId) {
        Azienda azienda = aziendaRepository.findById(aziendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Azienda " + aziendaId + " non trovata"));

        if (!aziendaFollowRepository.existsByFollowerIdAndAziendaId(followerId, aziendaId)) {
            aziendaFollowRepository.save(AziendaFollow.builder()
                    .follower(userRepository.getReferenceById(followerId))
                    .azienda(azienda)
                    .build());
            log.debug("Utente {} ha iniziato a seguire l'azienda {}", followerId, aziendaId);
        }
        return toStatusDto(followerId, aziendaId);
    }

    /**
     * Smette di seguire una pagina aziendale. Idempotente: se il follow non c'era non succede nulla.
     *
     * @param followerId utente autenticato (dal JWT)
     * @param aziendaId  azienda da smettere di seguire
     * @return lo stato aggiornato del follow
     */
    @Transactional
    public FollowStatusDto unfollow(Long followerId, Long aziendaId) {
        aziendaFollowRepository.deleteByFollowerIdAndAziendaId(followerId, aziendaId);
        aziendaFollowRepository.flush();
        return toStatusDto(followerId, aziendaId);
    }

    private FollowStatusDto toStatusDto(Long followerId, Long aziendaId) {
        return FollowStatusDto.builder()
                .followedByMe(aziendaFollowRepository.existsByFollowerIdAndAziendaId(followerId, aziendaId))
                .followersCount(aziendaFollowRepository.countByAziendaId(aziendaId))
                .build();
    }
}
