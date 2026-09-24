package com.ristorandoti.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Risposta di {@code POST /api/uploads/images}: l'URL pubblico dell'immagine caricata,
 * da usare poi come {@code mediaUrl}, {@code profilePictureUrl} o {@code bannerUrl}.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponseDto {

    private String url;
}
