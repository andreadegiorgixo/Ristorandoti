package com.ristorandoti.application.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Ruolo assegnabile a un dipendente per la gestione della pagina aziendale (tabella
 * {@code azienda_ruoli}, migration Flyway {@code V11__azienda_capability_roles.sql}).
 *
 * <p>Le capability associate sono lette dal database (tabella {@code azienda_ruolo_capabilities}),
 * non hardcoded nel codice: cambiare le capability di un ruolo esistente non richiede una
 * modifica applicativa, solo una riga di dati. Il proprietario dell'azienda non ha mai bisogno
 * di un ruolo qui: è sempre Admin implicito (vedi
 * {@link com.ristorandoti.application.service.AziendaPermissionService}).</p>
 */
@Entity
@Table(name = "azienda_ruoli")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "capabilities")
public class AziendaRuolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "codice", nullable = false, length = 30)
    private AziendaRuoloCodice codice;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    /** Capability comprese in questo ruolo. Un solo ruolo, mai più di una decina di righe: EAGER va bene. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "azienda_ruolo_capabilities", joinColumns = @JoinColumn(name = "ruolo_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "capability", length = 30)
    @Builder.Default
    private Set<Capability> capabilities = new HashSet<>();
}
