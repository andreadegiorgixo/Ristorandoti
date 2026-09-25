package com.ristorandoti.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.AziendaRuoloCodice;
import com.ristorandoti.application.entity.AziendaUserRuolo;
import com.ristorandoti.application.entity.Capability;

/** Repository Spring Data JPA per le assegnazioni di ruolo ({@link AziendaUserRuolo}). */
public interface AziendaUserRuoloRepository extends JpaRepository<AziendaUserRuolo, Long> {

    List<AziendaUserRuolo> findByAziendaIdAndDataRevocaIsNull(Long aziendaId);

    List<AziendaUserRuolo> findByAziendaIdAndUserIdAndDataRevocaIsNull(Long aziendaId, Long userId);

    Optional<AziendaUserRuolo> findByAziendaIdAndUserIdAndRuolo_CodiceAndDataRevocaIsNull(
            Long aziendaId, Long userId, AziendaRuoloCodice codice);

    long countByAziendaIdAndRuolo_CodiceAndDataRevocaIsNull(Long aziendaId, AziendaRuoloCodice codice);

    /**
     * Capability effettivamente attive per un dipendente: unione delle capability di tutti i suoi
     * ruoli non revocati, ma solo se il rapporto di assunzione con l'azienda risulta ancora in
     * corso (join con {@code Experience}). Questa condizione è verificata a ogni chiamata (non
     * solo quando il ruolo viene assegnato): così un rapporto di lavoro terminato disattiva
     * immediatamente l'accesso, anche se nessuno ha esplicitamente revocato il ruolo.
     *
     * @param aziendaId azienda su cui risolvere le capability
     * @param userId    dipendente di cui risolvere le capability
     * @return le capability attive, eventualmente vuote
     */
    @Query("""
            SELECT DISTINCT c FROM AziendaUserRuolo aur JOIN aur.ruolo r JOIN r.capabilities c
            WHERE aur.azienda.id = :aziendaId AND aur.user.id = :userId AND aur.dataRevoca IS NULL
            AND EXISTS (
                SELECT 1 FROM Experience e
                WHERE e.aziendaCollegata.id = :aziendaId AND e.profile.user.id = :userId AND e.dataEnd IS NULL
            )
            """)
    List<Capability> resolveActiveCapabilities(@Param("aziendaId") Long aziendaId, @Param("userId") Long userId);
}
