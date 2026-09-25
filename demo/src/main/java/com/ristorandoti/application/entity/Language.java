package com.ristorandoti.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Lingua conosciuta da un {@link Profile} (tabella {@code languages}), con livello separato
 * per scritto e parlato ({@link LanguageLevel}).
 */
@Entity
@Table(name = "languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "profile")
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Profilo a cui appartiene la lingua. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    /** Nome della lingua, es. "Inglese". */
    @Column(name = "lingua", nullable = false, length = 60)
    private String lingua;

    @Enumerated(EnumType.STRING)
    @Column(name = "livello_scritto", nullable = false, length = 30)
    private LanguageLevel livelloScritto;

    @Enumerated(EnumType.STRING)
    @Column(name = "livello_parlato", nullable = false, length = 30)
    private LanguageLevel livelloParlato;
}
