package com.ristorandoti.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ristorandoti.application.dto.UploadResponseDto;
import com.ristorandoti.application.service.FileStorageService;

import lombok.RequiredArgsConstructor;

/**
 * Caricamento delle immagini (foto dei post, foto profilo, banner). Richiede il JWT.
 *
 * <p>Il client carica prima il file qui e poi usa l'URL restituito nel body di
 * {@code POST /api/posts} o {@code PUT /api/profiles/me}.</p>
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    /**
     * @param file immagine (multipart, campo {@code file}), max 10 MB
     * @return {@code 201 Created} con l'URL pubblico dell'immagine, {@code 400} se il formato
     *         non è supportato, {@code 413} se il file è troppo grande
     */
    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDto> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponseDto(fileStorageService.storeImage(file)));
    }
}
