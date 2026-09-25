package com.ristorandoti.application.dto;

import java.util.List;

import com.ristorandoti.application.entity.AziendaRuoloCodice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dipendente attualmente assunto dall'azienda, con i ruoli di gestione pagina attualmente attivi. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AziendaDipendenteRuoliDto {

    private Long userId;

    private String name;

    private String profilePictureUrl;

    /** Ruoli attualmente attivi; lista vuota se il dipendente non ne ha nessuno. */
    private List<AziendaRuoloCodice> ruoli;
}
