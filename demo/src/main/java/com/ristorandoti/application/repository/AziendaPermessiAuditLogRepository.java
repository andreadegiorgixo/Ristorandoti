package com.ristorandoti.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.AziendaPermessiAuditLog;

/** Repository Spring Data JPA per il log di audit dei permessi ({@link AziendaPermessiAuditLog}). */
public interface AziendaPermessiAuditLogRepository extends JpaRepository<AziendaPermessiAuditLog, Long> {

    @EntityGraph(attributePaths = {"attore", "targetUser"})
    Page<AziendaPermessiAuditLog> findByAziendaIdOrderByDataEventoDesc(Long aziendaId, Pageable pageable);
}
