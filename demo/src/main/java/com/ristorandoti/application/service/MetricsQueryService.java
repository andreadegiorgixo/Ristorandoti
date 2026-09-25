package com.ristorandoti.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.DashboardMetricheDto;
import com.ristorandoti.application.dto.MetricSerieDto;
import com.ristorandoti.application.dto.MetricSeriePuntoDto;
import com.ristorandoti.application.dto.MetricaDashboard;
import com.ristorandoti.application.entity.AziendaMetricDaily;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.MetricaGiornaliera;
import com.ristorandoti.application.repository.AziendaMetricDailyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Query delle metriche aggregate per la Home della Dashboard: card (valore di periodo + variazione
 * percentuale rispetto al periodo precedente equivalente) e serie temporale per il grafico.
 *
 * <p>{@link MetricaDashboard#FOLLOWER} e {@link MetricaDashboard#LIKE} sono metriche "di stock":
 * l'aggregato in {@code azienda_metric_daily} conserva variazioni (+1/-1), non totali, quindi il
 * valore in un giorno è la somma cumulativa di tutte le variazioni fino a quel giorno (storia
 * completa dell'azienda, non solo il periodo richiesto). {@link MetricaDashboard#VISUALIZZATORI_UNICI}
 * e {@link MetricaDashboard#RICERCHE} sono metriche "di flusso": il valore in un giorno è il
 * conteggio di quel giorno, il totale di periodo è la somma dei giorni richiesti.</p>
 */
@Service
@RequiredArgsConstructor
public class MetricsQueryService {

    /** Limite di sicurezza sulla lunghezza dell'intervallo richiesto (poco più di 12 mesi). */
    private static final long MAX_GIORNI_INTERVALLO = 400;

    private static final Set<MetricaDashboard> METRICHE_CUMULATIVE = Set.of(MetricaDashboard.FOLLOWER, MetricaDashboard.LIKE);

    private final AziendaMetricDailyRepository repository;
    private final AziendaPermissionService aziendaPermissionService;

    /**
     * @param aziendaId     azienda di cui leggere le metriche
     * @param currentUserId utente autenticato (dal JWT)
     * @param dalRichiesto  inizio del periodo richiesto (incluso)
     * @param al            fine del periodo richiesto (incluso)
     * @param metriche      metriche richieste (una o più)
     * @return una serie per ogni metrica richiesta
     * @throws com.ristorandoti.application.exception.ResourceNotFoundException se l'azienda non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@link Capability#VIEW_DASHBOARD}
     */
    @Transactional(readOnly = true)
    public DashboardMetricheDto getMetriche(Long aziendaId, Long currentUserId, LocalDate dalRichiesto, LocalDate al,
                                            Set<MetricaDashboard> metriche) {
        aziendaPermissionService.ensureCapability(aziendaId, currentUserId, Capability.VIEW_DASHBOARD);

        LocalDate dal = dalRichiesto;
        if (al.isBefore(dal)) {
            dal = al;
        }
        if (ChronoUnit.DAYS.between(dal, al) > MAX_GIORNI_INTERVALLO) {
            dal = al.minusDays(MAX_GIORNI_INTERVALLO);
        }
        long giorniPeriodo = ChronoUnit.DAYS.between(dal, al) + 1;
        LocalDate dalPrecedente = dal.minusDays(giorniPeriodo);
        LocalDate alPrecedente = dal.minusDays(1);
        LocalDate datiDisponibiliDal = repository.findPrimoGiorno(aziendaId).orElse(null);

        List<MetricSerieDto> serie = new ArrayList<>();
        for (MetricaDashboard metrica : metriche) {
            serie.add(METRICHE_CUMULATIVE.contains(metrica)
                    ? serieCumulativa(aziendaId, metrica, dal, al, alPrecedente, datiDisponibiliDal)
                    : serieDiFlusso(aziendaId, metrica, dal, al, dalPrecedente, alPrecedente, datiDisponibiliDal));
        }
        return DashboardMetricheDto.builder().serie(serie).build();
    }

    private MetricSerieDto serieDiFlusso(Long aziendaId, MetricaDashboard metrica, LocalDate dal, LocalDate al,
                                         LocalDate dalPrecedente, LocalDate alPrecedente, LocalDate datiDisponibiliDal) {
        MetricaGiornaliera metricaStorage = metricaStorage(metrica);
        Map<LocalDate, Long> valoriPerGiorno = repository
                .findByAziendaIdAndMetricaAndGiornoBetweenOrderByGiornoAsc(aziendaId, metricaStorage, dal, al).stream()
                .collect(Collectors.toMap(AziendaMetricDaily::getGiorno, AziendaMetricDaily::getValore));
        long totalePrecedente = repository
                .findByAziendaIdAndMetricaAndGiornoBetweenOrderByGiornoAsc(aziendaId, metricaStorage, dalPrecedente, alPrecedente)
                .stream().mapToLong(AziendaMetricDaily::getValore).sum();

        List<MetricSeriePuntoDto> punti = new ArrayList<>();
        long totalePeriodo = 0;
        for (LocalDate giorno = dal; !giorno.isAfter(al); giorno = giorno.plusDays(1)) {
            long valore = valoriPerGiorno.getOrDefault(giorno, 0L);
            totalePeriodo += valore;
            punti.add(MetricSeriePuntoDto.builder().giorno(giorno).valore(valore).build());
        }

        return MetricSerieDto.builder()
                .metrica(metrica)
                .punti(punti)
                .totalePeriodo(totalePeriodo)
                .variazionePercento(variazionePercento(totalePeriodo, totalePrecedente))
                .datiDisponibiliDal(datiDisponibiliDal)
                .build();
    }

    private MetricSerieDto serieCumulativa(Long aziendaId, MetricaDashboard metrica, LocalDate dal, LocalDate al,
                                           LocalDate alPrecedente, LocalDate datiDisponibiliDal) {
        MetricaGiornaliera metricaStorage = metricaStorage(metrica);
        // Storia completa fino a "al": serve per calcolare il valore cumulativo in qualunque
        // giorno del periodo, non solo l'ultimo (l'aggregato conserva variazioni, non totali).
        List<AziendaMetricDaily> tutti = repository
                .findByAziendaIdAndMetricaAndGiornoLessThanEqualOrderByGiornoAsc(aziendaId, metricaStorage, al);

        long baseAlPrimoGiorno = tutti.stream()
                .filter(riga -> riga.getGiorno().isBefore(dal))
                .mapToLong(AziendaMetricDaily::getValore).sum();
        Map<LocalDate, Long> deltaPerGiorno = tutti.stream()
                .filter(riga -> !riga.getGiorno().isBefore(dal))
                .collect(Collectors.toMap(AziendaMetricDaily::getGiorno, AziendaMetricDaily::getValore, Long::sum));

        List<MetricSeriePuntoDto> punti = new ArrayList<>();
        long corrente = baseAlPrimoGiorno;
        long valoreAFinePeriodoPrecedente = tutti.stream()
                .filter(riga -> !riga.getGiorno().isAfter(alPrecedente))
                .mapToLong(AziendaMetricDaily::getValore).sum();
        for (LocalDate giorno = dal; !giorno.isAfter(al); giorno = giorno.plusDays(1)) {
            corrente += deltaPerGiorno.getOrDefault(giorno, 0L);
            punti.add(MetricSeriePuntoDto.builder().giorno(giorno).valore(corrente).build());
        }

        return MetricSerieDto.builder()
                .metrica(metrica)
                .punti(punti)
                .totalePeriodo(corrente)
                .variazionePercento(variazionePercento(corrente, valoreAFinePeriodoPrecedente))
                .datiDisponibiliDal(datiDisponibiliDal)
                .build();
    }

    private Double variazionePercento(long attuale, long precedente) {
        if (precedente == 0) {
            return null;
        }
        return (attuale - precedente) * 100.0 / precedente;
    }

    private MetricaGiornaliera metricaStorage(MetricaDashboard metrica) {
        return switch (metrica) {
            case VISUALIZZATORI_UNICI -> MetricaGiornaliera.VISUALIZZATORI_UNICI;
            case FOLLOWER -> MetricaGiornaliera.FOLLOWER_DELTA;
            case LIKE -> MetricaGiornaliera.LIKE_DELTA;
            case RICERCHE -> MetricaGiornaliera.RICERCHE;
        };
    }
}
