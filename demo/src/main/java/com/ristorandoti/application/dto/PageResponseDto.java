package com.ristorandoti.application.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Pagina di risultati in formato JSON stabile.
 *
 * <p>Si evita di restituire direttamente {@link Page} di Spring Data: la sua serializzazione
 * JSON non è garantita stabile tra le versioni (Spring stessa lo segnala con un warning).</p>
 *
 * @param content       elementi della pagina
 * @param page          indice della pagina (da 0)
 * @param size          dimensione della pagina
 * @param totalElements numero totale di elementi
 * @param totalPages    numero totale di pagine
 * @param last          {@code true} se è l'ultima pagina (utile per lo scroll infinito)
 * @param <T>           tipo degli elementi
 */
public record PageResponseDto<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                                 boolean last) {

    /**
     * @param page    pagina di Spring Data da cui leggere i metadati
     * @param content elementi già convertiti in DTO
     * @param <T>     tipo degli elementi
     * @return la pagina da serializzare
     */
    public static <T> PageResponseDto<T> of(Page<?> page, List<T> content) {
        return new PageResponseDto<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}
