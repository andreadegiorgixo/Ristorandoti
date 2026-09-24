package com.ristorandoti.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ristorandoti.application.exception.InvalidFileException;

import lombok.extern.slf4j.Slf4j;

/**
 * Salva le immagini caricate dagli utenti su disco, nella cartella {@code app.storage.local.dir},
 * servita pubblicamente sotto {@code /uploads/**} (vedi
 * {@link com.ristorandoti.application.config.WebConfig}).
 *
 * <p>Il formato si riconosce dai primi byte del file (i "magic bytes"), non dal nome né dal
 * Content-Type dichiarato dal client: così non si può caricare, ad esempio, un HTML rinominato
 * in {@code .jpg}. Il nome sul disco è un UUID generato dal server.</p>
 */
@Slf4j
@Service
public class FileStorageService {

    /** Formati ammessi, con la loro "firma" iniziale. */
    private enum ImageType {
        JPG("jpg"), PNG("png"), GIF("gif"), WEBP("webp");

        private final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }
    }

    private final Path rootDir;
    private final String publicBaseUrl;

    public FileStorageService(@Value("${app.storage.local.dir}") String dir,
                              @Value("${app.storage.local.public-base-url:}") String publicBaseUrl) {
        this.rootDir = Path.of(dir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile creare la cartella degli upload " + rootDir, e);
        }
    }

    public Path getRootDir() {
        return rootDir;
    }

    /**
     * @param file immagine inviata dal client (JPG, PNG, GIF o WebP)
     * @return URL assoluto con cui l'immagine è raggiungibile
     * @throws InvalidFileException se il file è vuoto o non è un'immagine supportata
     */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Seleziona un'immagine da caricare");
        }

        ImageType type = detectType(file);
        String filename = UUID.randomUUID() + "." + type.extension;

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, rootDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Salvataggio dell'immagine non riuscito", e);
        }
        log.debug("Immagine salvata: {} ({} byte)", filename, file.getSize());

        return baseUrl() + "/uploads/" + filename;
    }

    private ImageType detectType(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException e) {
            throw new UncheckedIOException("Lettura dell'immagine non riuscita", e);
        }

        if (startsWith(head, 0, 0xFF, 0xD8, 0xFF)) return ImageType.JPG;
        if (startsWith(head, 0, 0x89, 'P', 'N', 'G')) return ImageType.PNG;
        if (startsWith(head, 0, 'G', 'I', 'F', '8')) return ImageType.GIF;
        if (startsWith(head, 0, 'R', 'I', 'F', 'F') && startsWith(head, 8, 'W', 'E', 'B', 'P')) return ImageType.WEBP;

        throw new InvalidFileException("Formato non supportato: carica un'immagine JPG, PNG, GIF o WebP");
    }

    private static boolean startsWith(byte[] data, int offset, int... signature) {
        if (data.length < offset + signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((data[offset + i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    /** URL pubblico configurato oppure, se assente, quello con cui il client ha chiamato il server. */
    private String baseUrl() {
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "");
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
