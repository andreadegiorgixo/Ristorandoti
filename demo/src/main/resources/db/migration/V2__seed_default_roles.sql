-- =====================================================================
-- V2 - Ripristina i ruoli di base (idempotente)
-- Con ddl-auto=create Hibernate ricreava le tabelle dopo Flyway, svuotando
-- i ruoli inseriti in V1: senza ROLE_USER ogni registrazione fallisce.
-- MERGE inserisce il ruolo solo se non esiste già.
-- =====================================================================

MERGE INTO roles r
USING (SELECT 'ROLE_USER' AS name FROM dual) s
ON (r.name = s.name)
WHEN NOT MATCHED THEN INSERT (name) VALUES (s.name);

MERGE INTO roles r
USING (SELECT 'ROLE_ADMIN' AS name FROM dual) s
ON (r.name = s.name)
WHEN NOT MATCHED THEN INSERT (name) VALUES (s.name);
