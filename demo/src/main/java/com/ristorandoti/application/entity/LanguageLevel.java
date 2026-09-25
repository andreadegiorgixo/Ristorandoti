package com.ristorandoti.application.entity;

/**
 * Livello di conoscenza di una lingua, secondo una scala semplificata ispirata al QCER.
 * Usato separatamente per scritto e parlato in {@link Language}.
 *
 * <p>Per aggiungere un nuovo valore: aggiungere la costante qui E aggiornare il vincolo
 * {@code ck_languages_livello_scritto}/{@code ck_languages_livello_parlato} con una nuova
 * migration Flyway (vedi {@code V16__create_lingue_schema.sql}).</p>
 */
public enum LanguageLevel {
    /** Comprensione ed espressione di base, frasi semplici e informazioni essenziali. */
    BASE,
    /** Comunicazione autonoma su argomenti familiari e situazioni quotidiane. */
    INTERMEDIO,
    /** Uso fluente ed efficace in contesti di lavoro e professionali complessi. */
    CONOSCENZA_PROFESSIONALE,
    /** Lingua madre o padroneggiata a livello nativo. */
    MADRELINGUA
}
