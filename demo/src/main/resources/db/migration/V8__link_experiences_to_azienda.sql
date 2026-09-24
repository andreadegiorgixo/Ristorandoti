-- =====================================================================
-- V8 - Collegamento facoltativo tra un'esperienza lavorativa e un'azienda
-- registrata. Il nome resta comunque salvato come testo libero (azienda):
-- se non corrisponde a nessuna azienda esistente, il collegamento è NULL
-- e l'esperienza resta comunque visibile. Se l'azienda collegata viene
-- eliminata, il collegamento si azzera ma l'esperienza non viene toccata.
-- =====================================================================

ALTER TABLE experiences ADD azienda_id NUMBER(19);
ALTER TABLE experiences ADD CONSTRAINT fk_experiences_azienda
    FOREIGN KEY (azienda_id) REFERENCES aziende (id) ON DELETE SET NULL;

CREATE INDEX ix_experiences_azienda ON experiences (azienda_id);
