package com.ristorandoti.application.dto;

import java.util.Set;

import com.ristorandoti.application.entity.AziendaRuoloCodice;
import com.ristorandoti.application.entity.Capability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Definizione di un ruolo assegnabile e delle capability che comporta (legenda per la Dashboard). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaRuoloDto {

    private AziendaRuoloCodice codice;

    private String nome;

    private Set<Capability> capabilities;
}
