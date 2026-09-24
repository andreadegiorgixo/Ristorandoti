package com.ristorandoti.application.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entità JPA che rappresenta il profilo professionale di un utente (stile LinkedIn).
 *
 * <p>Relazione 1-1 con {@link User}: il profilo viene creato vuoto alla registrazione
 * (vedi {@link com.ristorandoti.application.service.ProfileService#createEmptyProfile}) e poi
 * completato dall'utente con {@code PUT /api/profiles/me}. Mappata sulla tabella {@code profiles}
 * (migration Flyway {@code V3__create_profile_schema.sql}).</p>
 *
 * <p>Esperienze e istruzione sono "possedute" dal profilo: {@link CascadeType#ALL} e
 * {@code orphanRemoval} fanno sì che, rimuovendo un elemento dalla lista, la riga venga
 * cancellata dal database senza bisogno di un repository dedicato.</p>
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "esperienze", "istruzione"}) // evita caricamenti lazy e cicli nei log
public class Profile {

    /** Chiave primaria, generata dal database (colonna IDENTITY su Oracle). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Utente proprietario del profilo (colonna {@code user_id}, UNIQUE). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** URL della foto profilo (quadrata). */
    @Column(name = "profile_picture_url", length = 1000)
    private String profilePictureUrl;

    /** URL del banner (rettangolare) mostrato in testa al profilo. */
    @Column(name = "banner_url", length = 1000)
    private String bannerUrl;

    /** Headline professionale, es. "Executive Chef presso Ristorante Da Mario". */
    @Column(name = "sommario", length = 500)
    private String sommario;

    /** Esperienze lavorative, dalla più recente. */
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataStart DESC")
    @Builder.Default
    private List<Experience> esperienze = new ArrayList<>();

    /** Percorsi di studio, dal più recente. */
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataStart DESC")
    @Builder.Default
    private List<Education> istruzione = new ArrayList<>();

    /**
     * Sostituisce tutte le esperienze mantenendo coerenti entrambi i lati della relazione.
     * Si modifica la lista esistente (e non la si riassegna) perché Hibernate traccia
     * quella istanza per applicare {@code orphanRemoval}.
     *
     * @param nuove nuove esperienze, non ancora collegate a nessun profilo
     */
    public void replaceEsperienze(List<Experience> nuove) {
        esperienze.clear();
        nuove.forEach(experience -> experience.setProfile(this));
        esperienze.addAll(nuove);
    }

    /**
     * Sostituisce tutti i percorsi di studio (stessa logica di {@link #replaceEsperienze}).
     *
     * @param nuove nuovi percorsi di studio, non ancora collegati a nessun profilo
     */
    public void replaceIstruzione(List<Education> nuove) {
        istruzione.clear();
        nuove.forEach(education -> education.setProfile(this));
        istruzione.addAll(nuove);
    }
}
