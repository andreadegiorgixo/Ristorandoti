package com.ristorandoti.application.entity;

import java.time.Instant;

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
@ToString(exclude = {"autore", "azienda"})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Utente che ha pubblicato il post (sempre una persona, anche per i post di una pagina aziendale). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autore_id", nullable = false)
    private User autore;

    /**
     * Pagina aziendale per cui il post è stato pubblicato; {@code null} per un post personale.
     * Quando valorizzato, il post appartiene alla home dell'azienda invece che al feed personale
     * dell'autore (vedi {@link com.ristorandoti.application.service.PostService}).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "azienda_id")
    private Azienda azienda;

    /** Testo del post; può mancare se c'è una foto. */
    @Column(name = "contenuto", length = 3000)
    private String contenuto;

    /** URL della foto (piatto, locale, attività); può mancare se c'è un testo. */
    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    /** Istante di pubblicazione, impostato automaticamente al primo salvataggio. */
    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    /**
     * Visibilità del post: {@code PUBBLICO} (default, sempre per i post personali) o
     * {@code PRIVATO} (nascosto dalla vista pubblica di un'azienda, ma ancora gestibile dalla
     * Dashboard). Vedi {@link com.ristorandoti.application.service.PostService}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibilita", nullable = false, length = 20)
    @Builder.Default
    private PostVisibilita visibilita = PostVisibilita.PUBBLICO;

    /** {@code null} finché il post non è stato rimosso (soft-delete: la riga resta per lo storico dei like). */
    @Column(name = "data_eliminazione")
    private Instant dataEliminazione;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }
}
