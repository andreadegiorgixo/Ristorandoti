-- =====================================================================
-- V15 - Sezione "Panoramica" della pagina aziendale. Riusa la colonna
-- esistente aziende.descrizione (già presente, già usata come testo
-- informale "chi siamo") invece di aggiungerne una nuova: l'unica
-- modifica necessaria è ampliarne il limite di lunghezza, in modo non
-- distruttivo (nessun dato esistente viene troncato o perso).
-- =====================================================================

ALTER TABLE aziende MODIFY descrizione VARCHAR2(4000 CHAR);
