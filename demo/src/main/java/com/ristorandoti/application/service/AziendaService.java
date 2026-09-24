package com.ristorandoti.application.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.AziendaDto;
import com.ristorandoti.application.dto.AziendaRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.AziendaMapper;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business delle aziende: creazione (qualsiasi utente autenticato, nessuna distinzione
 * di ruolo), lettura (singola, elenco generale, elenco di un utente), modifica ed eliminazione
 * (riservate al proprietario).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AziendaService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dataCreazione"), Sort.Order.desc("id"));

    private static final Sort NAME_ASC = Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id"));

    private final AziendaRepository aziendaRepository;
    private final UserRepository userRepository;
    private final AziendaMapper aziendaMapper;

    /**
     * Crea una nuova azienda a nome dell'utente autenticato. Nessuna restrizione di ruolo:
     * qualsiasi utente registrato può creare un'azienda.
     *
     * @param proprietarioId utente autenticato (dal JWT)
     * @param request        dati dell'azienda, già validati
     * @return l'azienda creata
     */
    @Transactional
    public AziendaDto create(Long proprietarioId, AziendaRequestDto request) {
        User proprietario = userRepository.findById(proprietarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente " + proprietarioId + " non trovato"));

        Azienda saved = aziendaRepository.save(aziendaMapper.toEntity(request, proprietario));
        log.debug("Azienda {} creata dall'utente {}", saved.getId(), proprietarioId);
        return aziendaMapper.toDto(saved);
    }

    /**
     * @param id id dell'azienda
     * @return l'azienda richiesta
     * @throws ResourceNotFoundException se non esiste
     */
    @Transactional(readOnly = true)
    public AziendaDto getById(Long id) {
        return aziendaMapper.toDto(findById(id));
    }

    /**
     * @param page indice della pagina (da 0)
     * @param size dimensione della pagina
     * @return una pagina di tutte le aziende, dalla più recente
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AziendaDto> getAll(int page, int size) {
        Page<Azienda> aziende = aziendaRepository.findAll(pageRequest(page, size));
        return PageResponseDto.of(aziende, aziende.getContent().stream().map(aziendaMapper::toDto).toList());
    }

    /**
     * @param proprietarioId utente di cui elencare le aziende
     * @param page           indice della pagina (da 0)
     * @param size           dimensione della pagina
     * @return una pagina delle aziende di quell'utente, dalla più recente
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AziendaDto> getByProprietario(Long proprietarioId, int page, int size) {
        if (!userRepository.existsById(proprietarioId)) {
            throw new ResourceNotFoundException("Utente " + proprietarioId + " non trovato");
        }
        Page<Azienda> aziende = aziendaRepository.findByProprietarioId(proprietarioId, pageRequest(page, size));
        return PageResponseDto.of(aziende, aziende.getContent().stream().map(aziendaMapper::toDto).toList());
    }

    /**
     * Ricerca le aziende il cui nome contiene il testo dato (senza distinzione maiuscole/minuscole),
     * in ordine alfabetico. Usata dal campo di ricerca quando si collega un'esperienza a un'azienda
     * esistente.
     *
     * @param query testo digitato dall'utente; se vuoto non viene eseguita nessuna ricerca
     * @param page  indice della pagina (da 0)
     * @param size  dimensione della pagina
     * @return una pagina delle aziende corrispondenti, in ordine alfabetico
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AziendaDto> search(String query, int page, int size) {
        String trimmed = query == null ? "" : query.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NAME_ASC);
        if (trimmed.isEmpty()) {
            return PageResponseDto.of(Page.empty(pageable), List.of());
        }
        Page<Azienda> aziende = aziendaRepository.findByNomeContainingIgnoreCase(trimmed, pageable);
        return PageResponseDto.of(aziende, aziende.getContent().stream().map(aziendaMapper::toDto).toList());
    }

    /**
     * Aggiorna un'azienda esistente. Solo il proprietario può farlo.
     *
     * @param id            id dell'azienda
     * @param currentUserId utente autenticato (dal JWT)
     * @param request       nuovi valori, già validati
     * @return l'azienda aggiornata
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non è il proprietario
     */
    @Transactional
    public AziendaDto update(Long id, Long currentUserId, AziendaRequestDto request) {
        Azienda azienda = findById(id);
        ensureOwner(azienda, currentUserId);

        aziendaMapper.updateEntity(azienda, request);
        Azienda saved = aziendaRepository.save(azienda);
        log.debug("Azienda {} aggiornata dall'utente {}", saved.getId(), currentUserId);
        return aziendaMapper.toDto(saved);
    }

    /**
     * Elimina un'azienda esistente. Solo il proprietario può farlo.
     *
     * @param id            id dell'azienda
     * @param currentUserId utente autenticato (dal JWT)
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non è il proprietario
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        Azienda azienda = findById(id);
        ensureOwner(azienda, currentUserId);

        aziendaRepository.delete(azienda);
        log.debug("Azienda {} eliminata dall'utente {}", id, currentUserId);
    }

    private Azienda findById(Long id) {
        return aziendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Azienda " + id + " non trovata"));
    }

    private void ensureOwner(Azienda azienda, Long currentUserId) {
        if (!azienda.getProprietario().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Solo il proprietario può modificare o eliminare questa azienda");
        }
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
