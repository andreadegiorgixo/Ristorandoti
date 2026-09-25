package com.ristorandoti.application.dto;

import java.time.Instant;

import com.ristorandoti.application.entity.StatoCandidatura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Candidatura a un'offerta di lavoro, restituita a HR/Admin (richiede {@code MANAGE_JOBS}). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidaturaDto {

    private Long id;

    private Long offertaId;

    private Long candidatoId;

    private String candidatoName;

    private String candidatoProfilePictureUrl;

    private String messaggio;

    private StatoCandidatura stato;

    private Instant dataCreazione;
}
