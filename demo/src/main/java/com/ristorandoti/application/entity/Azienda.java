package com.ristorandoti.application.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
 * Azienda (ristorante, locale, attività di ristorazione) creata da un utente della piattaforma
 * (tabella {@code aziende}, migration Flyway {@code V7__create_azienda_schema.sql}).
 *
 * <p>Relazione con {@link User}: un utente può creare più aziende (nessun vincolo UNIQUE su
 * {@code proprietario_id}); non esiste un concetto di "membri" oltre al proprietario.</p>
 */
@Entity
@Table(name = "aziende")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"proprietario", "servizi"})
public class Azienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Utente che ha creato l'azienda; unico autorizzato a modificarla o eliminarla. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private User proprietario;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoAzienda tipo;

    /** Testo della sezione "Panoramica" della pagina pubblica (vedi {@code app.dashboard.overview-max-length}). */
    @Column(name = "descrizione", length = 4000)
    private String descrizione;

    @Column(name = "indirizzo", length = 300)
    private String indirizzo;

    @Column(name = "citta", length = 100)
    private String citta;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "sito_web_url", length = 1000)
    private String sitoWebUrl;

    /** URL del logo aziendale (quadrato), come la foto profilo di un utente. */
    @Column(name = "foto_profilo_url", length = 1000)
    private String fotoProfiloUrl;

    /** URL del banner (rettangolare) mostrato in testa al profilo aziendale. */
    @Column(name = "banner_url", length = 1000)
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "fascia_prezzo", length = 20)
    private FasciaPrezzo fasciaPrezzo;

    /** Servizi offerti (tabella {@code azienda_servizi}, migration {@code V9}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "azienda_servizi", joinColumns = @JoinColumn(name = "azienda_id"))
    @Column(name = "servizio", length = 100)
    @Builder.Default
    private List<String> servizi = new ArrayList<>();

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione;

    @PrePersist
    void onCreate() {
        if (dataCreazione == null) {
            dataCreazione = Instant.now();
        }
    }

    /**
     * Sostituisce i servizi offerti mantenendo la stessa istanza di lista (Hibernate la traccia
     * per applicare correttamente le insert/delete sulla tabella {@code azienda_servizi}).
     *
     * @param nuovi nuovi servizi
     */
    public void replaceServizi(List<String> nuovi) {
        servizi.clear();
        servizi.addAll(nuovi);
    }
}
