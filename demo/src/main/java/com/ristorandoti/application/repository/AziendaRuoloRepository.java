package com.ristorandoti.application.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ristorandoti.application.entity.AziendaRuolo;
import com.ristorandoti.application.entity.AziendaRuoloCodice;

/** Repository Spring Data JPA per il catalogo dei ruoli assegnabili ({@link AziendaRuolo}). */
public interface AziendaRuoloRepository extends JpaRepository<AziendaRuolo, Long> {

    Optional<AziendaRuolo> findByCodice(AziendaRuoloCodice codice);
}
