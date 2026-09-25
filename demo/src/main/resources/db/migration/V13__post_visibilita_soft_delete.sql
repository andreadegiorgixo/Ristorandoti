-- =====================================================================
-- V13 - Visibilità e rimozione "morbida" dei post pubblicati come pagina
-- aziendale. I post personali non sono mai interessati: restano sempre
-- PUBBLICO e non vengono mai rimossi da questo flusso.
--
-- "Nascondere" cambia solo visibilita (PUBBLICO/PRIVATO), senza toccare
-- i like: un post nascosto conserva il proprio conteggio storico.
-- "Rimuovere" è un soft-delete (data_eliminazione valorizzata): i like
-- restano intatti anche in questo caso.
-- =====================================================================

ALTER TABLE posts ADD visibilita VARCHAR2(20 CHAR) DEFAULT 'PUBBLICO' NOT NULL;
ALTER TABLE posts ADD CONSTRAINT ck_posts_visibilita CHECK (visibilita IN ('PUBBLICO', 'PRIVATO'));
ALTER TABLE posts ADD data_eliminazione TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX ix_posts_azienda_stato ON posts (azienda_id, data_eliminazione, visibilita, data_creazione DESC);
