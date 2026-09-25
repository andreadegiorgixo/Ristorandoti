package com.ristorandoti.application.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.config.DashboardProperties;
import com.ristorandoti.application.dto.AziendaAuditLogDto;
import com.ristorandoti.application.dto.AziendaDipendenteRuoliDto;
import com.ristorandoti.application.dto.AziendaPermessiCorrentiDto;
import com.ristorandoti.application.dto.AziendaRuoloDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.AziendaPermessiAuditLog;
import com.ristorandoti.application.entity.AziendaRuolo;
import com.ristorandoti.application.entity.AziendaRuoloCodice;
import com.ristorandoti.application.entity.AziendaUserRuolo;
import com.ristorandoti.application.entity.AzioneAuditPermessi;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.Experience;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.InvalidRoleAssignmentException;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.repository.AziendaPermessiAuditLogRepository;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.AziendaRuoloRepository;
import com.ristorandoti.application.repository.AziendaUserRuoloRepository;
import com.ristorandoti.application.repository.ExperienceRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Punto unico di risoluzione ed enforcement delle capability sulla Dashboard aziendale.
 * Sostituisce il precedente controllo "autorizzato: sì/no" di {@code AziendaAutorizzazione} come
 * gate per le funzionalità della Dashboard (post, offerte di lavoro, panoramica, gestione permessi);
 * {@link AziendaService} mantiene la sua logica owner-only solo per modifica/eliminazione
 * dell'azienda stessa e dei dati anagrafici della pagina.
 *
 * <p><b>Proprietario:</b> ha sempre tutte le capability, senza bisogno di alcuna riga in
 * {@code azienda_user_ruoli} (Admin implicito, non revocabile).</p>
 *
 * <p><b>Auto-guarigione a lettura:</b> {@link #resolveCapabilities} verifica ad ogni chiamata,
 * tramite {@link AziendaUserRuoloRepository#resolveActiveCapabilities}, che il dipendente abbia
 * ancora un'esperienza lavorativa collegata all'azienda con {@code dataEnd IS NULL}. Se il
 * rapporto di lavoro termina, il primissimo controllo successivo nega già l'accesso, anche prima
 * che {@link #revokeAllRolesForEndedEmployment} chiuda esplicitamente le righe.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AziendaPermissionService {

    private static final int MAX_PAGE_SIZE = 50;

    private final AziendaRepository aziendaRepository;
    private final UserRepository userRepository;
    private final AziendaRuoloRepository aziendaRuoloRepository;
    private final AziendaUserRuoloRepository aziendaUserRuoloRepository;
    private final AziendaPermessiAuditLogRepository auditLogRepository;
    private final ExperienceRepository experienceRepository;
    private final DashboardProperties dashboardProperties;

    /**
     * @param aziendaId id dell'azienda
     * @param userId    utente da verificare
     * @return {@code true} se {@code userId} è il proprietario dell'azienda
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long aziendaId, Long userId) {
        return findAzienda(aziendaId).getProprietario().getId().equals(userId);
    }

    /**
     * @param aziendaId id dell'azienda
     * @param userId    utente di cui risolvere le capability
     * @return tutte le capability se {@code userId} è il proprietario, altrimenti l'unione delle
     *         capability dei ruoli attivi e con rapporto di lavoro verificato (può essere vuoto)
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public Set<Capability> resolveCapabilities(Long aziendaId, Long userId) {
        if (isOwner(aziendaId, userId)) {
            return EnumSet.allOf(Capability.class);
        }
        List<Capability> capabilities = aziendaUserRuoloRepository.resolveActiveCapabilities(aziendaId, userId);
        return capabilities.isEmpty() ? EnumSet.noneOf(Capability.class) : EnumSet.copyOf(capabilities);
    }

    @Transactional(readOnly = true)
    public boolean hasCapability(Long aziendaId, Long userId, Capability capability) {
        return resolveCapabilities(aziendaId, userId).contains(capability);
    }

    /**
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente non ha la capability richiesta
     */
    @Transactional(readOnly = true)
    public void ensureCapability(Long aziendaId, Long userId, Capability capability) {
        if (!hasCapability(aziendaId, userId, capability)) {
            throw new AccessDeniedException(
                    "Non hai il permesso di gestire questa sezione della pagina aziendale (" + capability + ")");
        }
    }

    /** Ruoli company-scoped attualmente attivi di un dipendente (il proprietario non ne ha bisogno). */
    @Transactional(readOnly = true)
    public List<AziendaRuoloCodice> ruoliAttivi(Long aziendaId, Long userId) {
        return aziendaUserRuoloRepository.findByAziendaIdAndUserIdAndDataRevocaIsNull(aziendaId, userId).stream()
                .map(aur -> aur.getRuolo().getCodice())
                .distinct()
                .toList();
    }

    /**
     * @param aziendaId id dell'azienda
     * @param userId    utente autenticato
     * @return le capability e i ruoli dell'utente su questa azienda, usati dal frontend per
     *         decidere quali sezioni della Dashboard mostrare in scrittura
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public AziendaPermessiCorrentiDto getPermessiCorrenti(Long aziendaId, Long userId) {
        boolean proprietario = isOwner(aziendaId, userId);
        Set<Capability> capabilities = proprietario
                ? EnumSet.allOf(Capability.class)
                : resolveCapabilities(aziendaId, userId);
        List<AziendaRuoloCodice> ruoli = proprietario ? List.of() : ruoliAttivi(aziendaId, userId);
        return AziendaPermessiCorrentiDto.builder()
                .proprietario(proprietario)
                .capabilities(capabilities)
                .ruoli(ruoli)
                .build();
    }

    /** Catalogo statico dei ruoli assegnabili e delle capability che comportano (legenda UI). */
    @Transactional(readOnly = true)
    public List<AziendaRuoloDto> catalogoRuoli() {
        return aziendaRuoloRepository.findAll().stream()
                .map(r -> AziendaRuoloDto.builder()
                        .codice(r.getCodice())
                        .nome(r.getNome())
                        .capabilities(r.getCapabilities())
                        .build())
                .toList();
    }

    /**
     * Dipendenti attualmente assunti dall'azienda (rapporto verificato), con i ruoli di gestione
     * pagina attualmente attivi (lista vuota se non ne hanno).
     *
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non ha {@link Capability#MANAGE_PERMISSIONS}
     */
    @Transactional(readOnly = true)
    public List<AziendaDipendenteRuoliDto> listaDipendentiConRuoli(Long aziendaId, Long currentUserId) {
        ensureCapability(aziendaId, currentUserId, Capability.MANAGE_PERMISSIONS);

        List<Experience> dipendenti = experienceRepository.findAllByAziendaCollegataIdAndDataEndIsNull(aziendaId);
        Map<Long, List<AziendaRuoloCodice>> ruoliPerUtente = aziendaUserRuoloRepository
                .findByAziendaIdAndDataRevocaIsNull(aziendaId).stream()
                .collect(Collectors.groupingBy(aur -> aur.getUser().getId(),
                        Collectors.mapping(aur -> aur.getRuolo().getCodice(), Collectors.toList())));

        return dipendenti.stream()
                .map(e -> {
                    User user = e.getProfile().getUser();
                    return AziendaDipendenteRuoliDto.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .profilePictureUrl(e.getProfile().getProfilePictureUrl())
                            .ruoli(ruoliPerUtente.getOrDefault(user.getId(), List.of()))
                            .build();
                })
                .sorted(Comparator.comparing(AziendaDipendenteRuoliDto::getName))
                .toList();
    }

    /**
     * Assegna un ruolo a un dipendente attualmente assunto dall'azienda. Idempotente: se il ruolo
     * è già attivo per quel dipendente, non fa nulla.
     *
     * @throws ResourceNotFoundException      se l'azienda o l'utente da promuovere non esistono
     * @throws AccessDeniedException          se l'utente autenticato non ha {@link Capability#MANAGE_PERMISSIONS}
     * @throws InvalidRoleAssignmentException se il destinatario non risulta attualmente assunto,
     *                                        oppure è già il proprietario (già Admin implicito)
     */
    @Transactional
    public void assignRole(Long aziendaId, Long actorId, Long targetUserId, AziendaRuoloCodice codice) {
        Azienda azienda = findAzienda(aziendaId);
        ensureCapability(aziendaId, actorId, Capability.MANAGE_PERMISSIONS);

        if (azienda.getProprietario().getId().equals(targetUserId)) {
            throw new InvalidRoleAssignmentException(
                    "Il proprietario è già Admin di questa pagina e non può ricevere ulteriori ruoli");
        }
        if (!experienceRepository.existsByAziendaCollegataIdAndProfileUserIdAndDataEndIsNull(aziendaId, targetUserId)) {
            throw new InvalidRoleAssignmentException(
                    "Solo chi risulta attualmente assunto da questa azienda può ricevere un ruolo");
        }
        if (aziendaUserRuoloRepository
                .findByAziendaIdAndUserIdAndRuolo_CodiceAndDataRevocaIsNull(aziendaId, targetUserId, codice)
                .isPresent()) {
            return;
        }

        AziendaRuolo ruolo = aziendaRuoloRepository.findByCodice(codice)
                .orElseThrow(() -> new ResourceNotFoundException("Ruolo " + codice + " non trovato"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente " + targetUserId + " non trovato"));
        User actor = userRepository.getReferenceById(actorId);

        aziendaUserRuoloRepository.save(AziendaUserRuolo.builder()
                .azienda(azienda)
                .user(target)
                .ruolo(ruolo)
                .assegnatoDa(actor)
                .build());
        registraAudit(azienda, actor, target, codice, AzioneAuditPermessi.ASSEGNATO, null);
        log.debug("Ruolo {} assegnato all'utente {} sull'azienda {} da {}", codice, targetUserId, aziendaId, actorId);
    }

    /**
     * Revoca un ruolo attivo di un dipendente. Idempotente (nessun errore se non era attivo).
     *
     * <p>Prima di revocare un ruolo Admin, verifica in transazione (con lock pessimistico
     * sull'azienda) che resti sempre almeno un Admin: garanzia già strutturale, dato che il
     * proprietario è Admin implicito e non revocabile, ma controllata comunque come difesa in
     * profondità in vista di una futura funzione di trasferimento della proprietà.</p>
     *
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non ha {@link Capability#MANAGE_PERMISSIONS}
     */
    @Transactional
    public void revokeRole(Long aziendaId, Long actorId, Long targetUserId, AziendaRuoloCodice codice) {
        ensureCapability(aziendaId, actorId, Capability.MANAGE_PERMISSIONS);
        // Lock pessimistico sulla riga azienda: serializza le operazioni concorrenti di gestione
        // permessi sulla stessa pagina, così il conteggio degli Admin attivi sotto è affidabile.
        Azienda azienda = aziendaRepository.findByIdForUpdate(aziendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Azienda " + aziendaId + " non trovata"));

        Optional<AziendaUserRuolo> riga = aziendaUserRuoloRepository
                .findByAziendaIdAndUserIdAndRuolo_CodiceAndDataRevocaIsNull(aziendaId, targetUserId, codice);
        if (riga.isEmpty()) {
            return;
        }
        if (dashboardProperties.isEnforceLastAdminGuard() && codice == AziendaRuoloCodice.ADMIN) {
            long altriAdminAttivi = aziendaUserRuoloRepository
                    .countByAziendaIdAndRuolo_CodiceAndDataRevocaIsNull(aziendaId, AziendaRuoloCodice.ADMIN) - 1;
            // Il proprietario è sempre un Admin implicito e non revocabile: questo conteggio non
            // può mai far scendere gli Admin a zero. Controllo mantenuto come rete di sicurezza
            // per una futura funzione di trasferimento della proprietà.
            if (altriAdminAttivi < 0) {
                throw new InvalidRoleAssignmentException("Deve sempre esistere almeno un Admin per questa pagina");
            }
        }

        User actor = userRepository.getReferenceById(actorId);
        AziendaUserRuolo assegnazione = riga.get();
        assegnazione.setDataRevoca(Instant.now());
        assegnazione.setRevocatoDa(actor);
        aziendaUserRuoloRepository.save(assegnazione);
        registraAudit(azienda, actor, assegnazione.getUser(), codice, AzioneAuditPermessi.REVOCATO, null);
        log.debug("Ruolo {} revocato all'utente {} sull'azienda {} da {}", codice, targetUserId, aziendaId, actorId);
    }

    /**
     * Chiude d'ufficio tutti i ruoli attivi di un dipendente su un'azienda quando il suo rapporto
     * di lavoro termina (invocato da {@link ProfileService} quando un'esperienza collegata a
     * quell'azienda perde il proprio stato "in corso"). Non è l'unica difesa:
     * {@link #resolveCapabilities} verifica comunque il rapporto ad ogni chiamata, quindi l'accesso
     * è già negato anche se questo metodo non venisse invocato; qui serve solo a chiudere
     * formalmente le righe (audit log, pulizia della UI di gestione permessi).
     *
     * @param aziendaId azienda il cui rapporto di lavoro con {@code userId} è terminato
     * @param userId    dipendente il cui rapporto è terminato
     */
    @Transactional
    public void revokeAllRolesForEndedEmployment(Long aziendaId, Long userId) {
        List<AziendaUserRuolo> attivi = aziendaUserRuoloRepository
                .findByAziendaIdAndUserIdAndDataRevocaIsNull(aziendaId, userId);
        if (attivi.isEmpty()) {
            return;
        }
        Azienda azienda = findAzienda(aziendaId);
        User target = userRepository.getReferenceById(userId);
        Instant now = Instant.now();
        for (AziendaUserRuolo assegnazione : attivi) {
            assegnazione.setDataRevoca(now);
            assegnazione.setRevocatoDa(target);
            aziendaUserRuoloRepository.save(assegnazione);
            registraAudit(azienda, target, target, assegnazione.getRuolo().getCodice(),
                    AzioneAuditPermessi.REVOCATO_FINE_RAPPORTO, "Rapporto di lavoro terminato");
        }
        log.debug("Ruoli sull'azienda {} revocati automaticamente per fine rapporto dell'utente {}", aziendaId, userId);
    }

    /**
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws AccessDeniedException     se l'utente autenticato non ha {@link Capability#MANAGE_PERMISSIONS}
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AziendaAuditLogDto> getAuditLog(Long aziendaId, Long currentUserId, int page, int size) {
        ensureCapability(aziendaId, currentUserId, Capability.MANAGE_PERMISSIONS);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<AziendaPermessiAuditLog> pagina = auditLogRepository.findByAziendaIdOrderByDataEventoDesc(aziendaId, pageable);
        List<AziendaAuditLogDto> content = pagina.getContent().stream().map(this::toAuditDto).toList();
        return PageResponseDto.of(pagina, content);
    }

    private void registraAudit(Azienda azienda, User attore, User target, AziendaRuoloCodice codice,
                               AzioneAuditPermessi azione, String dettaglio) {
        auditLogRepository.save(AziendaPermessiAuditLog.builder()
                .azienda(azienda)
                .attore(attore)
                .targetUser(target)
                .ruoloCodice(codice)
                .azione(azione)
                .dettaglio(dettaglio)
                .build());
    }

    private AziendaAuditLogDto toAuditDto(AziendaPermessiAuditLog entry) {
        return AziendaAuditLogDto.builder()
                .id(entry.getId())
                .attoreId(entry.getAttore().getId())
                .attoreName(entry.getAttore().getName())
                .targetUserId(entry.getTargetUser().getId())
                .targetUserName(entry.getTargetUser().getName())
                .ruoloCodice(entry.getRuoloCodice())
                .azione(entry.getAzione())
                .dettaglio(entry.getDettaglio())
                .dataEvento(entry.getDataEvento())
                .build();
    }

    private Azienda findAzienda(Long id) {
        return aziendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Azienda " + id + " non trovata"));
    }
}
