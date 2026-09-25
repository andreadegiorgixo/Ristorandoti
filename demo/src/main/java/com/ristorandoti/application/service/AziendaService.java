package com.ristorandoti.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.config.DashboardProperties;
import com.ristorandoti.application.dto.AziendaAutorizzazioneDto;
import com.ristorandoti.application.dto.AziendaDto;
import com.ristorandoti.application.dto.AziendaPersonaDto;
import com.ristorandoti.application.dto.AziendaRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.PanoramicaRequestDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.AziendaAutorizzazione;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.Experience;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.AziendaMapper;
import com.ristorandoti.application.repository.AziendaAutorizzazioneRepository;
import com.ristorandoti.application.repository.AziendaFollowRepository;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.ExperienceRepository;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.ristorandoti.application.mapper.ProfileMapper.blankToNull;

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

    /** Persone attualmente impiegate: dalla più recente come inizio esperienza. */
    private static final Sort CURRENT_EXPERIENCE_FIRST = Sort.by(Sort.Order.desc("dataStart"));

    private final AziendaRepository aziendaRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AziendaFollowRepository aziendaFollowRepository;
    private final AziendaAutorizzazioneRepository aziendaAutorizzazioneRepository;
    private final ExperienceRepository experienceRepository;
    private final AziendaMapper aziendaMapper;
    private final AziendaPermissionService aziendaPermissionService;
    private final MetricsService metricsService;
    private final DashboardProperties dashboardProperties;

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
     * @param id            id dell'azienda
     * @param currentUserId utente che fa la richiesta (per {@code followedByMe} e {@code gestibileDaMe})
     * @return l'azienda richiesta, con statistiche di follow e gestibilità
     * @throws ResourceNotFoundException se non esiste
     */
    @Transactional(readOnly = true)
    public AziendaDto getById(Long id, Long currentUserId) {
        Azienda azienda = findById(id);
        long followersCount = aziendaFollowRepository.countByAziendaId(id);
        boolean followedByMe = aziendaFollowRepository.existsByFollowerIdAndAziendaId(currentUserId, id);
        boolean gestibileDaMe = isManageable(azienda, currentUserId);
        boolean puoiVedereDashboard = aziendaPermissionService.hasCapability(id, currentUserId, Capability.VIEW_DASHBOARD);
        return aziendaMapper.toDto(azienda, followersCount, followedByMe, gestibileDaMe, puoiVedereDashboard);
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
     * <p>Punto centrale in cui si registra la metrica "Ricerche" (apparizione di un'azienda nei
     * risultati): tenerlo qui, in un solo posto, è ciò che rende facile cambiarne la definizione
     * in futuro (es. contare solo i click sui risultati) senza toccare altri punti del codice.</p>
     *
     * @param query         testo digitato dall'utente; se vuoto non viene eseguita nessuna ricerca
     * @param currentUserId utente che sta cercando (per la deduplica della metrica "Ricerche")
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina delle aziende corrispondenti, in ordine alfabetico
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AziendaDto> search(String query, Long currentUserId, int page, int size) {
        String trimmed = query == null ? "" : query.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NAME_ASC);
        if (trimmed.isEmpty()) {
            return PageResponseDto.of(Page.empty(pageable), List.of());
        }
        Page<Azienda> aziende = aziendaRepository.findByNomeContainingIgnoreCase(trimmed, pageable);
        aziende.getContent().forEach(azienda -> metricsService.recordSearchAppearance(azienda.getId(), currentUserId, trimmed));
        return PageResponseDto.of(aziende, aziende.getContent().stream().map(aziendaMapper::toDto).toList());
    }

    /**
     * Aggiorna i dati anagrafici di un'azienda esistente (nome, logo, copertina, ...). Riservato
     * al proprietario o, se {@code app.dashboard.page-settings-requires-manage-permissions} è
     * attivo (default), a chi ha {@code MANAGE_PERMISSIONS}: non è una delle capability elencate
     * nel brief, {@code MANAGE_PERMISSIONS} (tier Admin) è la più vicina.
     *
     * @param id            id dell'azienda
     * @param currentUserId utente autenticato (dal JWT)
     * @param request       nuovi valori, già validati
     * @return l'azienda aggiornata
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non può modificare i dati della pagina
     */
    @Transactional
    public AziendaDto update(Long id, Long currentUserId, AziendaRequestDto request) {
        Azienda azienda = findById(id);
        ensurePageSettingsEditable(azienda, currentUserId);

        aziendaMapper.updateEntity(azienda, request);
        Azienda saved = aziendaRepository.save(azienda);
        log.debug("Azienda {} aggiornata dall'utente {}", saved.getId(), currentUserId);
        return aziendaMapper.toDto(saved);
    }

    /**
     * Aggiorna il testo della sezione Panoramica. Richiede {@code MANAGE_OVERVIEW} (default:
     * solo Admin).
     *
     * @param id            id dell'azienda
     * @param currentUserId utente autenticato (dal JWT)
     * @param request       nuovo testo, già validato
     * @return l'azienda aggiornata
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non ha {@code MANAGE_OVERVIEW}
     */
    @Transactional
    public AziendaDto updatePanoramica(Long id, Long currentUserId, PanoramicaRequestDto request) {
        Azienda azienda = findById(id);
        aziendaPermissionService.ensureCapability(id, currentUserId, Capability.MANAGE_OVERVIEW);

        azienda.setDescrizione(blankToNull(request.getDescrizione()));
        Azienda saved = aziendaRepository.save(azienda);
        log.debug("Panoramica dell'azienda {} aggiornata dall'utente {}", id, currentUserId);
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

    /**
     * Persone che lavorano attualmente nell'azienda, ricavate dalle esperienze lavorative
     * correnti collegate (vedi {@link ExperienceRepository#findByAziendaCollegataIdAndDataEndIsNull}).
     *
     * @param aziendaId id dell'azienda
     * @param page      indice della pagina (da 0)
     * @param size      dimensione della pagina
     * @return una pagina delle persone, dalla più recente in base all'inizio dell'esperienza
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AziendaPersonaDto> getPersone(Long aziendaId, int page, int size) {
        if (!aziendaRepository.existsById(aziendaId)) {
            throw new ResourceNotFoundException("Azienda " + aziendaId + " non trovata");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                CURRENT_EXPERIENCE_FIRST);
        Page<Experience> esperienze = experienceRepository.findByAziendaCollegataIdAndDataEndIsNull(aziendaId, pageable);
        List<AziendaPersonaDto> content = esperienze.getContent().stream().map(this::toPersonaDto).toList();
        return PageResponseDto.of(esperienze, content);
    }

    /**
     * Autorizza una persona a gestire la pagina aziendale (pubblicare post, inserire offerte di
     * lavoro). Idempotente. Solo il proprietario può farlo.
     *
     * @param aziendaId        id dell'azienda
     * @param proprietarioId   utente autenticato (dal JWT)
     * @param userIdDaAutorizzare utente da autorizzare
     * @return la persona autorizzata
     * @throws ResourceNotFoundException se l'azienda o l'utente da autorizzare non esistono
     * @throws AccessDeniedException     se l'utente autenticato non è il proprietario
     */
    @Transactional
    public AziendaAutorizzazioneDto autorizza(Long aziendaId, Long proprietarioId, Long userIdDaAutorizzare) {
        Azienda azienda = findById(aziendaId);
        ensureOwner(azienda, proprietarioId);
        User user = userRepository.findById(userIdDaAutorizzare)
                .orElseThrow(() -> new ResourceNotFoundException("Utente " + userIdDaAutorizzare + " non trovato"));

        if (!aziendaAutorizzazioneRepository.existsByAziendaIdAndUserId(aziendaId, userIdDaAutorizzare)) {
            aziendaAutorizzazioneRepository.save(AziendaAutorizzazione.builder()
                    .azienda(azienda)
                    .user(user)
                    .build());
            log.debug("Utente {} autorizzato a gestire l'azienda {} dal proprietario {}",
                    userIdDaAutorizzare, aziendaId, proprietarioId);
        }
        return toAutorizzazioneDto(user, null);
    }

    /**
     * Revoca l'autorizzazione di una persona a gestire la pagina aziendale. Idempotente. Solo il
     * proprietario può farlo.
     *
     * @param aziendaId      id dell'azienda
     * @param proprietarioId utente autenticato (dal JWT)
     * @param userIdDaRevocare utente da revocare
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non è il proprietario
     */
    @Transactional
    public void revoca(Long aziendaId, Long proprietarioId, Long userIdDaRevocare) {
        Azienda azienda = findById(aziendaId);
        ensureOwner(azienda, proprietarioId);
        aziendaAutorizzazioneRepository.deleteByAziendaIdAndUserId(aziendaId, userIdDaRevocare);
        log.debug("Autorizzazione dell'utente {} sull'azienda {} revocata dal proprietario {}",
                userIdDaRevocare, aziendaId, proprietarioId);
    }

    /**
     * Elenco delle persone autorizzate a gestire la pagina aziendale (proprietario escluso: è
     * sempre autorizzato implicitamente). Solo il proprietario può leggerlo.
     *
     * @param aziendaId      id dell'azienda
     * @param proprietarioId utente autenticato (dal JWT)
     * @return le persone autorizzate
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non è il proprietario
     */
    @Transactional(readOnly = true)
    public List<AziendaAutorizzazioneDto> getAutorizzati(Long aziendaId, Long proprietarioId) {
        Azienda azienda = findById(aziendaId);
        ensureOwner(azienda, proprietarioId);

        List<AziendaAutorizzazione> autorizzazioni = aziendaAutorizzazioneRepository.findByAziendaId(aziendaId);
        if (autorizzazioni.isEmpty()) {
            return List.of();
        }
        Map<Long, Profile> profilesByUser = profileRepository
                .findByUserIdIn(autorizzazioni.stream().map(a -> a.getUser().getId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        return autorizzazioni.stream()
                .map(a -> toAutorizzazioneDto(a.getUser(), profilesByUser.get(a.getUser().getId())))
                .toList();
    }

    private AziendaPersonaDto toPersonaDto(Experience esperienza) {
        User user = esperienza.getProfile().getUser();
        return AziendaPersonaDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .profilePictureUrl(esperienza.getProfile().getProfilePictureUrl())
                .ruolo(esperienza.getRuolo())
                .build();
    }

    private AziendaAutorizzazioneDto toAutorizzazioneDto(User user, Profile profile) {
        return AziendaAutorizzazioneDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .profilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .build();
    }

    private boolean isManageable(Azienda azienda, Long userId) {
        return azienda.getProprietario().getId().equals(userId)
                || aziendaAutorizzazioneRepository.existsByAziendaIdAndUserId(azienda.getId(), userId);
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

    /**
     * Modificare i dati anagrafici della pagina non è una delle 5 capability del brief: per
     * default richiede il proprietario oppure {@code MANAGE_PERMISSIONS} (tier Admin, la
     * capability più vicina), configurabile con {@code app.dashboard.page-settings-requires-manage-permissions}.
     */
    private void ensurePageSettingsEditable(Azienda azienda, Long currentUserId) {
        if (azienda.getProprietario().getId().equals(currentUserId)) {
            return;
        }
        if (dashboardProperties.isPageSettingsRequiresManagePermissions()
                && aziendaPermissionService.hasCapability(azienda.getId(), currentUserId, Capability.MANAGE_PERMISSIONS)) {
            return;
        }
        throw new AccessDeniedException("Solo il proprietario o un Admin possono modificare i dati di questa pagina");
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
