package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stato del "follow" restituito da {@code POST/DELETE /api/follows/{userId}}: permette al client
 * di aggiornare bottone e contatore senza ricaricare l'intero profilo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowStatusDto {

    /** {@code true} se, dopo l'operazione, l'utente autenticato segue l'utente indicato. */
    private boolean followedByMe;

    /** Numero aggiornato di follower dell'utente indicato. */
    private long followersCount;
}
