package com.ristorandoti.application.dto;

import java.util.List;
import java.util.Set;

import com.ristorandoti.application.entity.AziendaRuoloCodice;
import com.ristorandoti.application.entity.Capability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Capability e ruoli dell'utente autenticato su una specifica azienda. Usato dal frontend per
 * decidere quali sezioni della Dashboard mostrare in scrittura e quali in sola lettura.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaPermessiCorrentiDto {

    /** {@code true} se l'utente è il proprietario (Admin implicito con tutte le capability). */
    private boolean proprietario;

    private Set<Capability> capabilities;

    /** Ruoli company-scoped attivi; vuoto per il proprietario (non ne ha bisogno). */
    private List<AziendaRuoloCodice> ruoli;
}
