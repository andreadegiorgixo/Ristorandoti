package com.ristorandoti.application.entity;

/**
 * Visibilità di un post pubblicato come pagina aziendale (i post personali sono sempre
 * {@link #PUBBLICO}). "Nascondere" un post lo porta a {@link #PRIVATO} senza cancellarlo: resta
 * visibile e gestibile dalla Dashboard, ma non compare più nella vista pubblica dell'azienda.
 */
public enum PostVisibilita {
    PUBBLICO,
    PRIVATO
}
