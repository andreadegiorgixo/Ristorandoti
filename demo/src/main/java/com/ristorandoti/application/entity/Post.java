package com.ristorandoti.application.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Post pubblicato da un utente nel feed della community (tabella {@code posts},
 * migration Flyway {@code V4__create_post_schema.sql}).
 *
 * <p>I like non sono una collezione dell'entità: vengono contati con query aggregate in
 * {@link com.ristorandoti.application.repository.PostLikeRepository}, così caricare una pagina
 * di feed non richiede di leggere tutti i like di ogni post.</p>
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "autore")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Utente che ha pubblicato il post. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autore_id", nullable = false)
    private User autore;

    /** Testo del post; può mancare se c'è una foto. */
    @Column(name = "contenuto", length = 3000)
    private String contenuto;

    /** URL della foto (piatto, locale, attività); può mancare se c'è un testo. */
    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    /** Istante di pubblicazione, impostato automaticamente al primo salvataggio. */
    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
