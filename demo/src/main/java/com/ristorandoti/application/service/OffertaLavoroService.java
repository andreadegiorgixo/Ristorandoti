package com.ristorandoti.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.config.DashboardProperties;
import com.ristorandoti.application.dto.OffertaLavoroDto;
import com.ristorandoti.application.dto.OffertaLavoroRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.OffertaLavoro;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidOffertaLavoroException;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.OffertaLavoroMapper;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.CandidaturaLavoroRepository;
import com.ristorandoti.application.repository.OffertaLavoroRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business delle offerte di lavoro: pubblicazione, lettura, modifica e chiusura.
 *
 * <p>Ogni offerta scade {@code app.dashboard.job-duration-days} giorni dopo la pubblicazione
 * ({@link OffertaLavoro#getDataScadenza()}, calcolata qui). Lo stato effettivo si ricalcola
 * sempre a lettura confrontando la scadenza con l'istante corrente (vedi
 * {@link OffertaLavoroMapper}): la colonna {@code stato} è solo una cache aggiornata dallo
 * scheduler di pulizia ({@link OffertaLavoroCleanupJob}), mai l'unica fonte di verità.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OffertaLavoroService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dataCreazione"), Sort.Order.desc("id"));

    private final OffertaLavoroRepository offertaLavoroRepository;
    private final CandidaturaLavoroRepository candidaturaLavoroRepository;
    private final AziendaRepository aziendaRepository;
    private final UserRepository userRepository;
    private final AziendaPermissionService aziendaPermissionService;
    private final DashboardProperties dashboardProperties;
    private final OffertaLavoroMapper offertaLavoroMapper;

    /**
     * Pubblica una nuova offerta di lavoro per un'azienda. Il limite di offerte contemporanee è
     * applicato in modo transazionale: un lock pessimistico sulla riga azienda serializza le
     * richieste concorrenti, così due pubblicazioni simultanee non possono insieme superare il
     * limite (un semplice "conta poi inserisci" senza lock ne sarebbe invece esposto).
     *
     * @param aziendaId id dell'azienda
     * @param userId    utente autenticato (dal JWT)
     * @param request   dati dell'offerta, già validati
     * @return l'offerta creata
     * @throws ResourceNotFoundException     se l'azienda non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_JOBS}
     * @throws InvalidOffertaLavoroException se l'azienda ha già raggiunto il limite di offerte attive
     */
    @Transactional
    public OffertaLavoroDto create(Long aziendaId, Long userId, OffertaLavoroRequestDto request) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_JOBS);
        Azienda azienda = aziendaRepository.findByIdForUpdate(aziendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Azienda " + aziendaId + " non trovata"));

        Instant now = Instant.now();
        long attive = offertaLavoroRepository.countByAziendaIdAndDataScadenzaAfter(aziendaId, now);
        int limite = dashboardProperties.getMaxOfferteAttive();
        if (attive >= limite) {
            throw new InvalidOffertaLavoroException(
                    "Puoi avere al massimo " + limite + " offerte di lavoro attive contemporaneamente");
        }

        User autore = userRepository.getReferenceById(userId);
        OffertaLavoro offerta = offertaLavoroMapper.toEntity(request, azienda, autore);
        offerta.setDataScadenza(now.plus(dashboardProperties.getJobDurationDays(), ChronoUnit.DAYS));

        OffertaLavoro saved = offertaLavoroRepository.save(offerta);
        log.debug("Offerta di lavoro {} pubblicata per l'azienda {} dall'utente {}", saved.getId(), aziendaId, userId);
        return toDto(saved, userId);
    }

    /**
     * Vista pubblica: solo le offerte non ancora scadute.
     *
     * @param aziendaId     id dell'azienda
     * @param currentUserId utente che fa la richiesta (per {@code candidaturaGiaInviata})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina delle offerte di lavoro attive dell'azienda, dalla più recente
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<OffertaLavoroDto> getByAzienda(Long aziendaId, Long currentUserId, int page, int size) {
        if (!aziendaRepository.existsById(aziendaId)) {
            throw new ResourceNotFoundException("Azienda " + aziendaId + " non trovata");
        }
        Page<OffertaLavoro> offerte = offertaLavoroRepository
                .findByAziendaIdAndDataScadenzaAfter(aziendaId, Instant.now(), pageRequest(page, size));
        return toPageResponse(offerte, currentUserId);
    }

    /**
     * Vista Dashboard: tutte le offerte (attive e scadute), per lo storico. Richiede solo
     * {@code VIEW_DASHBOARD} (lettura): chi non può gestirle le vede comunque, in sola lettura.
     *
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code VIEW_DASHBOARD}
     */
    @Transactional(readOnly = true)
    public PageResponseDto<OffertaLavoroDto> getByAziendaDashboard(Long aziendaId, Long currentUserId, int page, int size) {
        aziendaPermissionService.ensureCapability(aziendaId, currentUserId, Capability.VIEW_DASHBOARD);
        Page<OffertaLavoro> offerte = offertaLavoroRepository.findByAziendaId(aziendaId, pageRequest(page, size));
        return toPageResponse(offerte, currentUserId);
    }

    /**
     * Modifica titolo/descrizione di un'offerta esistente. Se nel frattempo è scaduta, rifiuta
     * con un messaggio comprensibile invece di salvare modifiche su un'offerta non più attiva.
     *
     * @throws ResourceNotFoundException se l'offerta non esiste o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_JOBS}
     * @throws InvalidOffertaLavoroException se l'offerta è scaduta nel frattempo
     */
    @Transactional
    public OffertaLavoroDto update(Long aziendaId, Long offertaId, Long userId, OffertaLavoroRequestDto request) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_JOBS);
        OffertaLavoro offerta = findOfAzienda(aziendaId, offertaId);
        if (offerta.getDataScadenza().isBefore(Instant.now())) {
            throw new InvalidOffertaLavoroException("Questa offerta è scaduta nel frattempo: aggiorna la pagina");
        }
        offertaLavoroMapper.updateEntity(offerta, request);
        log.debug("Offerta di lavoro {} modificata sull'azienda {} dall'utente {}", offertaId, aziendaId, userId);
        return toDto(offerta, userId);
    }

    /**
     * Chiude (elimina) un'offerta di lavoro, liberando uno slot per una nuova. Le candidature
     * collegate vengono cancellate a cascata dal database.
     *
     * @throws ResourceNotFoundException se l'offerta non esiste o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_JOBS}
     */
    @Transactional
    public void chiudi(Long aziendaId, Long offertaId, Long userId) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_JOBS);
        OffertaLavoro offerta = findOfAzienda(aziendaId, offertaId);
        offertaLavoroRepository.delete(offerta);
        log.debug("Offerta di lavoro {} chiusa sull'azienda {} dall'utente {}", offertaId, aziendaId, userId);
    }

    /**
     * Ricerca globale (tutte le aziende) delle offerte attive il cui titolo contiene il testo
     * dato, dalla più recente. Usata dalla ricerca globale in navbar.
     *
     * @param query         testo digitato dall'utente; se vuoto non viene eseguita nessuna ricerca
     * @param currentUserId utente che fa la richiesta (per {@code candidaturaGiaInviata})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina delle offerte corrispondenti, dalla più recente
     */
    @Transactional(readOnly = true)
    public PageResponseDto<OffertaLavoroDto> search(String query, Long currentUserId, int page, int size) {
        String trimmed = query == null ? "" : query.trim();
        Pageable pageable = pageRequest(page, size);
        if (trimmed.isEmpty()) {
            return PageResponseDto.of(Page.empty(pageable), List.of());
        }
        Page<OffertaLavoro> offerte = offertaLavoroRepository
                .findByTitoloContainingIgnoreCaseAndDataScadenzaAfter(trimmed, Instant.now(), pageable);
        return toPageResponse(offerte, currentUserId);
    }

    /**
     * @throws ResourceNotFoundException se l'offerta non esiste o non appartiene a questa azienda (protezione IDOR)
     */
    OffertaLavoro findOfAzienda(Long aziendaId, Long offertaId) {
        OffertaLavoro offerta = offertaLavoroRepository.findById(offertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Offerta di lavoro " + offertaId + " non trovata"));
        if (!offerta.getAzienda().getId().equals(aziendaId)) {
            throw new ResourceNotFoundException("Offerta di lavoro " + offertaId + " non trovata per l'azienda " + aziendaId);
        }
        return offerta;
    }

    private PageResponseDto<OffertaLavoroDto> toPageResponse(Page<OffertaLavoro> offerte, Long currentUserId) {
        return PageResponseDto.of(offerte, offerte.getContent().stream().map(o -> toDto(o, currentUserId)).toList());
    }

    private OffertaLavoroDto toDto(OffertaLavoro offerta, Long currentUserId) {
        boolean giaCandidato = candidaturaLavoroRepository.existsByOffertaIdAndCandidatoId(offerta.getId(), currentUserId);
        return offertaLavoroMapper.toDto(offerta, giaCandidato);
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
