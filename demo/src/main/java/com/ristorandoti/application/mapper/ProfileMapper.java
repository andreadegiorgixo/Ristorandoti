package com.ristorandoti.application.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ristorandoti.application.dto.EducationDto;
import com.ristorandoti.application.dto.EducationRequestDto;
import com.ristorandoti.application.dto.ExperienceDto;
import com.ristorandoti.application.dto.ExperienceRequestDto;
import com.ristorandoti.application.dto.ProfileDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.Education;
import com.ristorandoti.application.entity.Experience;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.repository.AziendaRepository;

/**
 * Mapper tra l'entità {@link Profile} (con esperienze e istruzione) e i relativi DTO.
 * Scritto a mano per lo stesso motivo di {@link UserMapper}.
 *
 * <p>Esperienze e istruzione sono sempre ordinate dalla più recente, anche subito dopo un
 * aggiornamento (quando la lista è ancora nell'ordine di invio del client).</p>
 *
 * <p>{@link #toDto} accede alle collezioni lazy: va invocato dentro una transazione
 * (lo fa {@link com.ristorandoti.application.service.ProfileService}).</p>
 */
@Component
public class ProfileMapper {

    private final AziendaRepository aziendaRepository;

    public ProfileMapper(AziendaRepository aziendaRepository) {
        this.aziendaRepository = aziendaRepository;
    }

    /**
     * @param profile          profilo con utente già caricato
     * @param followersCount   numero di follower dell'utente
     * @param followingCount   numero di utenti seguiti
     * @param followedByMe     se l'utente che fa la richiesta segue questo profilo
     * @param recensioniCount  numero di recensioni ricevute
     * @param valutazioneMedia media dei voti ricevuti, {@code null} se nessuna recensione
     * @return il DTO completo del profilo
     */
    public ProfileDto toDto(Profile profile, long followersCount, long followingCount, boolean followedByMe,
                            long recensioniCount, Double valutazioneMedia) {
        return ProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .bannerUrl(profile.getBannerUrl())
                .sommario(profile.getSommario())
                .esperienze(profile.getEsperienze().stream()
                        .sorted(Comparator.comparing(Experience::getDataStart).reversed())
                        .map(this::toDto).toList())
                .istruzione(profile.getIstruzione().stream()
                        .sorted(Comparator.comparing(Education::getDataStart).reversed())
                        .map(this::toDto).toList())
                .followersCount(followersCount)
                .followingCount(followingCount)
                .followedByMe(followedByMe)
                .recensioniCount(recensioniCount)
                .valutazioneMedia(valutazioneMedia)
                .build();
    }

    public ExperienceDto toDto(Experience experience) {
        Azienda aziendaCollegata = experience.getAziendaCollegata();
        return ExperienceDto.builder()
                .id(experience.getId())
                .azienda(experience.getAzienda())
                .aziendaId(aziendaCollegata != null ? aziendaCollegata.getId() : null)
                .ruolo(experience.getRuolo())
                .dataStart(experience.getDataStart())
                .dataEnd(experience.getDataEnd())
                .descrizione(experience.getDescrizione())
                .build();
    }

    public EducationDto toDto(Education education) {
        return EducationDto.builder()
                .id(education.getId())
                .istituto(education.getIstituto())
                .titoloStudio(education.getTitoloStudio())
                .dataStart(education.getDataStart())
                .dataEnd(education.getDataEnd())
                .build();
    }

    /**
     * Converte le esperienze ricevute in nuove entità, non ancora collegate al profilo
     * (lo fa {@link Profile#replaceEsperienze}).
     *
     * <p>Se {@link ExperienceRequestDto#getAziendaId()} è valorizzato, l'esperienza viene
     * collegata all'azienda registrata corrispondente. Se l'id non corrisponde più a nessuna
     * azienda (es. eliminata nel frattempo), il collegamento resta vuoto: l'esperienza si salva
     * comunque con il solo nome libero.</p>
     *
     * @param dtos esperienze già validate
     * @return nuove entità {@link Experience}
     */
    public List<Experience> toExperiences(List<ExperienceRequestDto> dtos) {
        return dtos.stream()
                .map(dto -> Experience.builder()
                        .azienda(dto.getAzienda().trim())
                        .aziendaCollegata(dto.getAziendaId() != null
                                ? aziendaRepository.findById(dto.getAziendaId()).orElse(null)
                                : null)
                        .ruolo(dto.getRuolo().trim())
                        .dataStart(dto.getDataStart())
                        .dataEnd(dto.getDataEnd())
                        .descrizione(blankToNull(dto.getDescrizione()))
                        .build())
                .toList();
    }

    /**
     * Converte i percorsi di studio ricevuti in nuove entità, non ancora collegate al profilo
     * (lo fa {@link Profile#replaceIstruzione}).
     *
     * @param dtos percorsi di studio già validati
     * @return nuove entità {@link Education}
     */
    public List<Education> toEducations(List<EducationRequestDto> dtos) {
        return dtos.stream()
                .map(dto -> Education.builder()
                        .istituto(dto.getIstituto().trim())
                        .titoloStudio(dto.getTitoloStudio().trim())
                        .dataStart(dto.getDataStart())
                        .dataEnd(dto.getDataEnd())
                        .build())
                .toList();
    }

    /**
     * @param value stringa ricevuta dal client
     * @return la stringa senza spazi ai bordi, oppure {@code null} se vuota
     */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
