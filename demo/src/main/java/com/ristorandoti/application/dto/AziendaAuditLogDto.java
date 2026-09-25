package com.ristorandoti.application.dto;

import java.time.Instant;

import com.ristorandoti.application.entity.AziendaRuoloCodice;
import com.ristorandoti.application.entity.AzioneAuditPermessi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Voce del log di audit delle modifiche ai permessi, restituita a {@code GET .../permessi/audit}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaAuditLogDto {

    private Long id;

    private Long attoreId;

    private String attoreName;

    private Long targetUserId;

    private String targetUserName;

    private AziendaRuoloCodice ruoloCodice;

    private AzioneAuditPermessi azione;

    private String dettaglio;

    private Instant dataEvento;
}
