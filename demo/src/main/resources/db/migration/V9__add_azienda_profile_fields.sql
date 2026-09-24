-- =====================================================================
-- V9 - Profilo aziendale: foto profilo (logo), banner, fascia di prezzo
-- e servizi offerti, sullo stesso modello del profilo utente.
-- =====================================================================

ALTER TABLE aziende ADD foto_profilo_url VARCHAR2(1000 CHAR);
ALTER TABLE aziende ADD banner_url VARCHAR2(1000 CHAR);
ALTER TABLE aziende ADD fascia_prezzo VARCHAR2(20 CHAR);
ALTER TABLE aziende ADD CONSTRAINT ck_aziende_fascia_prezzo
    CHECK (fascia_prezzo IN ('EURO_1', 'EURO_2', 'EURO_3', 'EURO_4', 'EURO_5'));

CREATE TABLE azienda_servizi (
    azienda_id NUMBER(19) NOT NULL,
    servizio   VARCHAR2(100 CHAR) NOT NULL,
    CONSTRAINT fk_azienda_servizi_azienda FOREIGN KEY (azienda_id) REFERENCES aziende (id) ON DELETE CASCADE
);
CREATE INDEX ix_azienda_servizi_azienda ON azienda_servizi (azienda_id);
