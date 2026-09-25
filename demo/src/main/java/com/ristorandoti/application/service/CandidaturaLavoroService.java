package com.ristorandoti.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.CandidaturaDto;
import com.ristorandoti.application.dto.CandidaturaRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.CandidaturaLavoro;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidCandidaturaException;
import com.ristorandoti.application.repository.CandidaturaLavoroRepository;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.ristorandoti.application.mapper.ProfileMapper.blankToNull;

/**
 * Flusso minimo di candidatura a un'offerta di lavoro: nessuna versione precedente esisteva nel
 * modello dati, questa è la prima implementazione (candidarsi, ed elenco candidati per HR/Admin).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidaturaLavoroService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CandidaturaLavoroRepository candidaturaLavoroRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final OffertaLavoroService offertaLavoroService;
    private final AziendaPermissionService aziendaPermissionService;

    /**
     * Invia una candidatura per un'offerta di lavoro. Chiunque può candidarsi (non serve essere
     * dipendente dell'azienda): idempotente solo nel senso che una seconda candidatura per la
     * stessa offerta viene rifiutata, non silenziosamente ignorata (l'utente deve sapere che ha
     * già inviato la propria candidatura).
     *
     * @throws com.ristorandoti.application.exception.ResourceNotFoundException se l'offerta non
     *         esiste o non appartiene a questa azienda
     * @throws InvalidCandidaturaException se l'offerta è scaduta o l'utente si è già candidato
     */
    @Transactional
    public CandidaturaDto candidati(Long aziendaId, Long offertaId, Long candidatoId, CandidaturaRequestDto request) {
        OffertaLavoro offerta = offertaLavoroService.findOfAzienda(aziendaId, offertaId);
        if (offerta.getDataScadenza().isBefore(Instant.now())) {
            throw new InvalidCandidaturaException("Questa offerta di lavoro è scaduta");
        }
        if (candidaturaLavoroRepository.existsByOffertaIdAndCandidatoId(offertaId, candidatoId)) {
            throw new InvalidCandidaturaException("Ti sei già candidato per questa offerta");
        }

        User candidato = userRepository.getReferenceById(candidatoId);
        CandidaturaLavoro candidatura = candidaturaLavoroRepository.save(CandidaturaLavoro.builder()
                .offerta(offerta)
                .candidato(candidato)
                .messaggio(blankToNull(request.getMessaggio()))
                .build());
        log.debug("Utente {} candidato per l'offerta {} (azienda {})", candidatoId, offertaId, aziendaId);
        return toDto(candidatura, candidato.getName(), null);
    }

    /**
     * Elenco dei candidati per un'offerta, dal più recente. Richiede {@code MANAGE_JOBS}.
     *
     * @throws com.ristorandoti.application.exception.ResourceNotFoundException se l'offerta non
     *         esiste o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_JOBS}
     */
    @Transactional(readOnly = true)
    public PageResponseDto<CandidaturaDto> getCandidati(Long aziendaId, Long offertaId, Long currentUserId, int page, int size) {
        aziendaPermissionService.ensureCapability(aziendaId, currentUserId, Capability.MANAGE_JOBS);
        offertaLavoroService.findOfAzienda(aziendaId, offertaId);

        Page<CandidaturaLavoro> pagina = candidaturaLavoroRepository
                .findByOffertaIdOrderByDataCreazioneDesc(offertaId, pageRequest(page, size));
        List<CandidaturaLavoro> candidature = pagina.getContent();
        if (candidature.isEmpty()) {
            return PageResponseDto.of(pagina, List.of());
        }

        Set<Long> candidatoIds = candidature.stream().map(c -> c.getCandidato().getId()).collect(Collectors.toSet());
        Map<Long, Profile> profiliPerUtente = profileRepository.findByUserIdIn(candidatoIds).stream()
                .collect(Collectors.toMap(profilo -> profilo.getUser().getId(), Function.identity()));

        List<CandidaturaDto> content = candidature.stream()
                .map(c -> {
                    Profile profilo = profiliPerUtente.get(c.getCandidato().getId());
                    return toDto(c, c.getCandidato().getName(), profilo != null ? profilo.getProfilePictureUrl() : null);
                })
                .toList();
        return PageResponseDto.of(pagina, content);
    }

    private CandidaturaDto toDto(CandidaturaLavoro candidatura, String candidatoName, String candidatoProfilePictureUrl) {
        return CandidaturaDto.builder()
                .id(candidatura.getId())
                .offertaId(candidatura.getOfferta().getId())
                .candidatoId(candidatura.getCandidato().getId())
                .candidatoName(candidatoName)
                .candidatoProfilePictureUrl(candidatoProfilePictureUrl)
                .messaggio(candidatura.getMessaggio())
                .stato(candidatura.getStato())
                .dataCreazione(candidatura.getDataCreazione())
                .build();
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }
}
