package com.ristorandoti.application.service;

import java.time.LocalDate;
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

import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.ReviewDto;
import com.ristorandoti.application.dto.ReviewEligibilityDto;
import com.ristorandoti.application.dto.ReviewRequestDto;
import com.ristorandoti.application.entity.Experience;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.Review;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidReviewException;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.ReviewMapper;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.ReviewRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business delle recensioni tra colleghi: un utente può recensirne un altro solo se
 * hanno lavorato nello stesso locale in un periodo di tempo sovrapposto (requisito di sicurezza
 * verificato in {@link #hasOverlappingExperience}, MAI delegato al solo client).
 *
 * <p>Il "locale" non è un'entità propria: nelle esperienze del profilo l'azienda è testo libero
 * ({@link Experience#getAzienda()}), quindi il confronto è per uguaglianza case-insensitive del
 * nome, non per id. Con dati puliti (stesso nome scritto allo stesso modo) funziona bene; refusi
 * o denominazioni diverse dello stesso locale non vengono riconosciuti come lo stesso posto.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dataCreazione"), Sort.Order.desc("id"));

    private final ReviewRepository reviewRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    /**
     * @param autoreId      utente autenticato che vorrebbe recensire
     * @param destinatarioId utente da recensire
     * @return se può recensirlo (sovrapposizione lavorativa) e se lo ha già fatto
     * @throws ResourceNotFoundException se il destinatario non esiste
     */
    @Transactional(readOnly = true)
    public ReviewEligibilityDto getEligibility(Long autoreId, Long destinatarioId) {
        if (!userRepository.existsById(destinatarioId)) {
            throw new ResourceNotFoundException("Utente " + destinatarioId + " non trovato");
        }
        boolean self = autoreId.equals(destinatarioId);
        boolean alreadyReviewed = reviewRepository.findByAutoreIdAndDestinatarioId(autoreId, destinatarioId).isPresent();
        return ReviewEligibilityDto.builder()
                .canReview(!self && hasOverlappingExperience(autoreId, destinatarioId))
                .alreadyReviewed(alreadyReviewed)
                .build();
    }

    /**
     * Crea la recensione di {@code autoreId} verso {@code destinatarioId}, oppure aggiorna
     * quella già esistente (una sola recensione per coppia autore/destinatario).
     *
     * @param autoreId      utente autenticato (dal JWT)
     * @param destinatarioId utente recensito
     * @param request       voto e testo, già validati
     * @return la recensione salvata
     * @throws InvalidReviewException     se {@code autoreId} coincide con {@code destinatarioId}
     *                                    o se non c'è sovrapposizione lavorativa tra i due
     * @throws ResourceNotFoundException se il destinatario non esiste
     */
    @Transactional
    public ReviewDto upsert(Long autoreId, Long destinatarioId, ReviewRequestDto request) {
        if (autoreId.equals(destinatarioId)) {
            throw new InvalidReviewException("Non puoi recensire te stesso");
        }
        User destinatario = userRepository.findById(destinatarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente " + destinatarioId + " non trovato"));

        // Verifica server-side: non fidarsi del pulsante disabilitato lato client.
        if (!hasOverlappingExperience(autoreId, destinatarioId)) {
            throw new InvalidReviewException(
                    "Puoi recensire solo chi ha lavorato con te nello stesso locale in un periodo sovrapposto");
        }

        Review review = reviewRepository.findByAutoreIdAndDestinatarioId(autoreId, destinatarioId)
                .orElseGet(() -> Review.builder()
                        .autore(userRepository.getReferenceById(autoreId))
                        .destinatario(destinatario)
                        .build());
        review.setValutazione(request.getValutazione());
        review.setTesto(request.getTesto().trim());

        Review saved = reviewRepository.save(review);
        log.debug("Recensione {} dell'utente {} verso l'utente {} salvata", saved.getId(), autoreId, destinatarioId);
        return reviewMapper.toDto(saved, profileRepository.findByUserId(autoreId).orElse(null));
    }

    /**
     * Elimina la recensione che {@code autoreId} ha lasciato a {@code destinatarioId}.
     * Idempotente: se non esisteva non succede nulla.
     *
     * @param autoreId      utente autenticato (dal JWT)
     * @param destinatarioId destinatario della recensione da eliminare
     */
    @Transactional
    public void delete(Long autoreId, Long destinatarioId) {
        reviewRepository.deleteByAutoreIdAndDestinatarioId(autoreId, destinatarioId);
    }

    /**
     * @param userId utente di cui leggere le recensioni ricevute
     * @param page   indice della pagina (da 0)
     * @param size   dimensione della pagina
     * @return una pagina delle recensioni ricevute, dalla più recente
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<ReviewDto> getReviewsForUser(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Utente " + userId + " non trovato");
        }
        Page<Review> reviews = reviewRepository.findByDestinatarioId(userId, pageRequest(page, size));
        return PageResponseDto.of(reviews, toDtos(reviews.getContent()));
    }

    /**
     * Requisito fondamentale: {@code userId1} e {@code userId2} possono recensirsi solo se hanno
     * almeno un'esperienza nello stesso locale (stesso nome azienda, case-insensitive) con un
     * periodo in comune. {@code dataEnd == null} significa "tuttora in corso": si tratta come
     * un intervallo aperto verso il futuro.
     *
     * @param userId1 primo utente
     * @param userId2 secondo utente
     * @return {@code true} se esiste almeno una coppia di esperienze sovrapposte
     */
    private boolean hasOverlappingExperience(Long userId1, Long userId2) {
        List<Experience> esperienze1 = esperienzeOf(userId1);
        if (esperienze1.isEmpty()) {
            return false;
        }
        List<Experience> esperienze2 = esperienzeOf(userId2);
        if (esperienze2.isEmpty()) {
            return false;
        }
        for (Experience e1 : esperienze1) {
            for (Experience e2 : esperienze2) {
                if (sameAzienda(e1, e2) && periodsOverlap(e1, e2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Experience> esperienzeOf(Long userId) {
        return profileRepository.findByUserId(userId).map(Profile::getEsperienze).orElse(List.of());
    }

    private static boolean sameAzienda(Experience a, Experience b) {
        return a.getAzienda().trim().equalsIgnoreCase(b.getAzienda().trim());
    }

    private static boolean periodsOverlap(Experience a, Experience b) {
        LocalDate endA = a.getDataEnd() != null ? a.getDataEnd() : LocalDate.MAX;
        LocalDate endB = b.getDataEnd() != null ? b.getDataEnd() : LocalDate.MAX;
        return !a.getDataStart().isAfter(endB) && !b.getDataStart().isAfter(endA);
    }

    /**
     * Converte una pagina di recensioni in DTO con una sola query in blocco (profili degli autori).
     */
    private List<ReviewDto> toDtos(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return List.of(); // Oracle non accetta "IN ()"
        }

        Set<Long> autoreIds = reviews.stream().map(review -> review.getAutore().getId()).collect(Collectors.toSet());
        Map<Long, Profile> profilesByUser = profileRepository.findByUserIdIn(autoreIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));

        return reviews.stream()
                .map(review -> reviewMapper.toDto(review, profilesByUser.get(review.getAutore().getId())))
                .toList();
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
