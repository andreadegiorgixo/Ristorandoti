package com.ristorandoti.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.OffertaLavoroDto;
import com.ristorandoti.application.dto.OffertaLavoroRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidOffertaLavoroException;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.OffertaLavoroMapper;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.OffertaLavoroRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business delle offerte di lavoro: pubblicazione, lettura ed eliminazione.
 *
 * <p>Non esiste un campo "attiva" sull'entità: un'offerta chiusa viene eliminata, quindi
 * "offerte attive" equivale al numero di righe presenti per l'azienda. Il limite di
 * {@value #MAX_OFFERTE_ATTIVE} offerte contemporanee è controllato qui, non solo lato client:
 * vedi {@link #create}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OffertaLavoroService {

    /** Numero massimo di offerte di lavoro attive per azienda, richiesto dal prodotto. */
    private static final int MAX_OFFERTE_ATTIVE = 3;

    private static final int MAX_PAGE_SIZE = 50;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dataCreazione"), Sort.Order.desc("id"));

    private final OffertaLavoroRepository offertaLavoroRepository;
    private final AziendaRepository aziendaRepository;
    private final UserRepository userRepository;
    private final AziendaService aziendaService;
    private final OffertaLavoroMapper offertaLavoroMapper;

    /**
     * Pubblica una nuova offerta di lavoro per un'azienda. Solo il proprietario o una persona
     * autorizzata possono farlo (vedi {@link AziendaService#ensureManageable}).
     *
     * @param aziendaId id dell'azienda
     * @param userId    utente autenticato (dal JWT)
     * @param request   dati dell'offerta, già validati
     * @return l'offerta creata
     * @throws ResourceNotFoundException     se l'azienda non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non è
     *         proprietario né autorizzato
     * @throws InvalidOffertaLavoroException se l'azienda ha già {@value #MAX_OFFERTE_ATTIVE} offerte attive
     */
    @Transactional
    public OffertaLavoroDto create(Long aziendaId, Long userId, OffertaLavoroRequestDto request) {
        aziendaService.ensureManageable(aziendaId, userId);

        if (offertaLavoroRepository.countByAziendaId(aziendaId) >= MAX_OFFERTE_ATTIVE) {
            throw new InvalidOffertaLavoroException(
                    "Puoi avere al massimo " + MAX_OFFERTE_ATTIVE + " offerte di lavoro attive contemporaneamente");
        }

        Azienda azienda = aziendaRepository.getReferenceById(aziendaId);
        User autore = userRepository.getReferenceById(userId);
        OffertaLavoro saved = offertaLavoroRepository.save(offertaLavoroMapper.toEntity(request, azienda, autore));
        log.debug("Offerta di lavoro {} pubblicata per l'azienda {} dall'utente {}", saved.getId(), aziendaId, userId);
        return offertaLavoroMapper.toDto(saved);
    }

    /**
     * @param aziendaId id dell'azienda
     * @param page      indice della pagina (da 0)
     * @param size      dimensione della pagina
     * @return una pagina delle offerte di lavoro attive dell'azienda, dalla più recente
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<OffertaLavoroDto> getByAzienda(Long aziendaId, int page, int size) {
        if (!aziendaRepository.existsById(aziendaId)) {
            throw new ResourceNotFoundException("Azienda " + aziendaId + " non trovata");
        }
        Page<OffertaLavoro> offerte = offertaLavoroRepository.findByAziendaId(aziendaId, pageRequest(page, size));
        return PageResponseDto.of(offerte, offerte.getContent().stream().map(offertaLavoroMapper::toDto).toList());
    }

    /**
     * Chiude (elimina) un'offerta di lavoro, liberando uno slot per una nuova. Solo il
     * proprietario o una persona autorizzata della relativa azienda possono farlo.
     *
     * @param offertaId id dell'offerta
     * @param userId    utente autenticato (dal JWT)
     * @throws ResourceNotFoundException se l'offerta non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non è
     *         proprietario né autorizzato per l'azienda dell'offerta
     */
    @Transactional
    public void chiudi(Long offertaId, Long userId) {
        OffertaLavoro offerta = offertaLavoroRepository.findById(offertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Offerta di lavoro " + offertaId + " non trovata"));
        aziendaService.ensureManageable(offerta.getAzienda().getId(), userId);

        offertaLavoroRepository.delete(offerta);
        log.debug("Offerta di lavoro {} chiusa dall'utente {}", offertaId, userId);
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
