package com.ristorandoti.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ristorandoti.application.dto.CreatePostRequestDto;
import com.ristorandoti.application.dto.PageResponseDto;
import com.ristorandoti.application.dto.PostDto;
import com.ristorandoti.application.entity.Azienda;
import com.ristorandoti.application.entity.Capability;
import com.ristorandoti.application.entity.Post;
import com.ristorandoti.application.entity.PostLike;
import com.ristorandoti.application.entity.PostVisibilita;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.PostMapper;
import com.ristorandoti.application.repository.AziendaRepository;
import com.ristorandoti.application.repository.PostLikeRepository;
import com.ristorandoti.application.repository.PostRepository;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business dei post: feed della community, post di un utente, post di una pagina
 * aziendale, pubblicazione e like.
 *
 * <p>Un post può essere personale (feed e profilo dell'autore) oppure pubblicato come pagina
 * aziendale ({@link Post#getAzienda()} valorizzato): i due mondi non si mescolano, vedi
 * {@link PostRepository}.</p>
 *
 * <p>Per trasformare una pagina di post in DTO servono solo tre query aggiuntive in totale
 * (profili degli autori, conteggio like, like dell'utente corrente), indipendentemente dal
 * numero di post: vedi {@link #toDtos}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    /** Dimensione massima di una pagina, per evitare richieste troppo pesanti. */
    private static final int MAX_PAGE_SIZE = 50;

    /** Dal più recente; a parità di istante vince l'id più alto, così la paginazione è stabile. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dataCreazione"), Sort.Order.desc("id"));

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final AziendaRepository aziendaRepository;
    private final AziendaPermissionService aziendaPermissionService;
    private final PostMapper postMapper;
    private final MetricsService metricsService;

    /**
     * @param currentUserId utente che richiede il feed (per {@code likedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina (limitata a {@value #MAX_PAGE_SIZE})
     * @return una pagina del feed globale (solo post personali), dal post più recente
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostDto> getFeed(Long currentUserId, int page, int size) {
        Page<Post> posts = postRepository.findByAziendaIdIsNullAndDataEliminazioneIsNull(pageRequest(page, size));
        return PageResponseDto.of(posts, toDtos(posts.getContent(), currentUserId));
    }

    /**
     * @param userId        autore di cui leggere i post
     * @param currentUserId utente che fa la richiesta (per {@code likedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina dei post personali dell'autore, dal più recente
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostDto> getPostsByUser(Long userId, Long currentUserId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Utente " + userId + " non trovato");
        }
        Page<Post> posts = postRepository.findByAutoreIdAndAziendaIdIsNullAndDataEliminazioneIsNull(userId, pageRequest(page, size));
        return PageResponseDto.of(posts, toDtos(posts.getContent(), currentUserId));
    }

    /**
     * Vista pubblica dei post di un'azienda: solo quelli {@code PUBBLICO} (mai i rimossi). Un post
     * nascosto non è raggiungibile da qui nemmeno conoscendone l'id, perché questa query non lo
     * restituisce affatto.
     *
     * @param aziendaId     azienda di cui leggere i post pubblicati come pagina
     * @param currentUserId utente che fa la richiesta (per {@code likedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina dei post pubblici dell'azienda, dal più recente
     * @throws ResourceNotFoundException se l'azienda non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostDto> getPostsByAzienda(Long aziendaId, Long currentUserId, int page, int size) {
        if (!aziendaRepository.existsById(aziendaId)) {
            throw new ResourceNotFoundException("Azienda " + aziendaId + " non trovata");
        }
        Page<Post> posts = postRepository
                .findByAziendaIdAndVisibilitaAndDataEliminazioneIsNull(aziendaId, PostVisibilita.PUBBLICO, pageRequest(page, size));
        return PageResponseDto.of(posts, toDtos(posts.getContent(), currentUserId));
    }

    /**
     * Vista Dashboard dei post di un'azienda: sia pubblici sia privati (mai i rimossi), con filtro
     * opzionale per stato. Richiede solo {@code VIEW_DASHBOARD} (lettura), non {@code MANAGE_POSTS}:
     * chi non può gestire i post li vede comunque, in sola lettura, come da matrice di default.
     *
     * @param aziendaId     azienda di cui leggere i post
     * @param currentUserId utente autenticato (dal JWT)
     * @param filtroStato   {@code null} per tutti gli stati, altrimenti solo quello indicato
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina dei post dell'azienda, dal più recente
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code VIEW_DASHBOARD}
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostDto> getPostsByAziendaDashboard(Long aziendaId, Long currentUserId,
                                                                PostVisibilita filtroStato, int page, int size) {
        aziendaPermissionService.ensureCapability(aziendaId, currentUserId, Capability.VIEW_DASHBOARD);
        Page<Post> posts = filtroStato != null
                ? postRepository.findByAziendaIdAndVisibilitaAndDataEliminazioneIsNull(aziendaId, filtroStato, pageRequest(page, size))
                : postRepository.findByAziendaIdAndDataEliminazioneIsNull(aziendaId, pageRequest(page, size));
        return PageResponseDto.of(posts, toDtos(posts.getContent(), currentUserId));
    }

    /**
     * Pubblica un nuovo post a nome dell'utente autenticato.
     *
     * @param userId  autore (dal JWT)
     * @param request testo e/o foto, già validati
     * @return il post creato
     */
    @Transactional
    public PostDto createPost(Long userId, CreatePostRequestDto request) {
        User autore = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente " + userId + " non trovato"));

        Post saved = postRepository.save(postMapper.toEntity(request, autore));
        log.debug("Post {} pubblicato dall'utente {}", saved.getId(), userId);
        return toDto(saved, userId);
    }

    /**
     * Pubblica un nuovo post come pagina aziendale. Richiede {@link Capability#MANAGE_POSTS}
     * (vedi {@link AziendaPermissionService}). L'autore resta l'utente che pubblica, per
     * tracciabilità; il post compare nella home dell'azienda, non nel feed personale dell'autore.
     *
     * @param aziendaId azienda per cui pubblicare
     * @param userId    utente autenticato (dal JWT)
     * @param request   testo e/o foto, già validati
     * @return il post creato
     * @throws ResourceNotFoundException se l'azienda non esiste
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non è
     *         proprietario né autorizzato
     */
    @Transactional
    public PostDto createAziendaPost(Long aziendaId, Long userId, CreatePostRequestDto request) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_POSTS);

        Azienda azienda = aziendaRepository.getReferenceById(aziendaId);
        User autore = userRepository.getReferenceById(userId);
        Post post = postMapper.toEntity(request, autore);
        post.setAzienda(azienda);

        Post saved = postRepository.save(post);
        log.debug("Post {} pubblicato per l'azienda {} dall'utente {}", saved.getId(), aziendaId, userId);
        return toDto(saved, userId);
    }

    /**
     * Mette like a un post. Idempotente: un secondo like dello stesso utente non ha effetto.
     *
     * @param postId post a cui mettere like
     * @param userId utente autenticato
     * @return il post con il conteggio aggiornato
     * @throws ResourceNotFoundException se il post non esiste
     */
    @Transactional
    public PostDto like(Long postId, Long userId) {
        Post post = findVisiblePost(postId, userId);
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .user(userRepository.getReferenceById(userId))
                    .build());
            if (post.getAzienda() != null) {
                metricsService.recordLikeDelta(post.getAzienda().getId(), 1);
            }
        }
        return toDto(post, userId);
    }

    /**
     * Toglie il like a un post. Idempotente: se il like non c'era non succede nulla.
     *
     * @param postId post da cui togliere il like
     * @param userId utente autenticato
     * @return il post con il conteggio aggiornato
     * @throws ResourceNotFoundException se il post non esiste
     */
    @Transactional
    public PostDto unlike(Long postId, Long userId) {
        Post post = findVisiblePost(postId, userId);
        long righeCancellate = postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        postLikeRepository.flush();
        if (righeCancellate > 0 && post.getAzienda() != null) {
            metricsService.recordLikeDelta(post.getAzienda().getId(), -1);
        }
        return toDto(post, userId);
    }

    /**
     * Modifica testo/foto di un post della pagina aziendale già pubblicato.
     *
     * @throws ResourceNotFoundException se il post non esiste, è già stato rimosso o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_POSTS}
     */
    @Transactional
    public PostDto updateAziendaPost(Long aziendaId, Long postId, Long userId, CreatePostRequestDto request) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_POSTS);
        Post post = findPostOfAzienda(aziendaId, postId);
        postMapper.updateEntity(post, request);
        log.debug("Post {} modificato sull'azienda {} dall'utente {}", postId, aziendaId, userId);
        return toDto(post, userId);
    }

    /**
     * Nasconde un post della pagina aziendale (visibilità {@code PRIVATO}): resta gestibile dalla
     * Dashboard e i suoi like restano nel totale storico, ma non compare più nella vista pubblica.
     *
     * @throws ResourceNotFoundException se il post non esiste, è già stato rimosso o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_POSTS}
     */
    @Transactional
    public void hide(Long aziendaId, Long postId, Long userId) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_POSTS);
        findPostOfAzienda(aziendaId, postId).setVisibilita(PostVisibilita.PRIVATO);
        log.debug("Post {} nascosto sull'azienda {} dall'utente {}", postId, aziendaId, userId);
    }

    /**
     * Rende di nuovo pubblico un post precedentemente nascosto.
     *
     * @throws ResourceNotFoundException se il post non esiste, è già stato rimosso o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_POSTS}
     */
    @Transactional
    public void unhide(Long aziendaId, Long postId, Long userId) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_POSTS);
        findPostOfAzienda(aziendaId, postId).setVisibilita(PostVisibilita.PUBBLICO);
        log.debug("Post {} reso di nuovo pubblico sull'azienda {} dall'utente {}", postId, aziendaId, userId);
    }

    /**
     * Rimuove un post della pagina aziendale (soft-delete: la riga resta per non perdere lo
     * storico dei like, ma sparisce da ogni lista, pubblica e Dashboard).
     *
     * @throws ResourceNotFoundException se il post non esiste, è già stato rimosso o non appartiene a questa azienda
     * @throws org.springframework.security.access.AccessDeniedException se l'utente non ha {@code MANAGE_POSTS}
     */
    @Transactional
    public void delete(Long aziendaId, Long postId, Long userId) {
        aziendaPermissionService.ensureCapability(aziendaId, userId, Capability.MANAGE_POSTS);
        findPostOfAzienda(aziendaId, postId).setDataEliminazione(Instant.now());
        log.debug("Post {} rimosso sull'azienda {} dall'utente {}", postId, aziendaId, userId);
    }

    /**
     * Post di un'utente qualsiasi (like/unlike): un post nascosto non deve essere raggiungibile
     * nemmeno conoscendone l'id da chi non ha almeno {@code VIEW_DASHBOARD} sull'azienda che lo ha
     * pubblicato. Risponde {@code 404}, non {@code 403}, per non rivelarne l'esistenza.
     *
     * @throws ResourceNotFoundException se il post non esiste, è stato rimosso, oppure è privato
     *         e l'utente non ha accesso alla Dashboard di quell'azienda
     */
    private Post findVisiblePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getDataEliminazione() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Post " + postId + " non trovato"));
        if (post.getAzienda() != null && post.getVisibilita() == PostVisibilita.PRIVATO
                && !aziendaPermissionService.hasCapability(post.getAzienda().getId(), userId, Capability.VIEW_DASHBOARD)) {
            throw new ResourceNotFoundException("Post " + postId + " non trovato");
        }
        return post;
    }

    /**
     * Post di una specifica azienda, per le mutazioni della Dashboard (nascondi/mostra/rimuovi):
     * verifica anche che il post appartenga davvero a {@code aziendaId} (protezione IDOR), non solo
     * che l'id esista.
     *
     * @throws ResourceNotFoundException se il post non esiste, è già stato rimosso, oppure non
     *         appartiene a questa azienda (o è un post personale)
     */
    private Post findPostOfAzienda(Long aziendaId, Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getDataEliminazione() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Post " + postId + " non trovato"));
        if (post.getAzienda() == null || !post.getAzienda().getId().equals(aziendaId)) {
            throw new ResourceNotFoundException("Post " + postId + " non trovato per l'azienda " + aziendaId);
        }
        return post;
    }

    private PostDto toDto(Post post, Long currentUserId) {
        Profile profile = profileRepository.findByUserId(post.getAutore().getId()).orElse(null);
        long likeCount = postLikeRepository.countByPostId(post.getId());
        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
        return postMapper.toDto(post, profile, likeCount, likedByMe);
    }

    /**
     * Converte una pagina di post in DTO con tre query in blocco (profili, conteggi, like propri).
     */
    private List<PostDto> toDtos(List<Post> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return List.of(); // Oracle non accetta "IN ()"
        }

        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Set<Long> autoreIds = posts.stream().map(post -> post.getAutore().getId()).collect(Collectors.toSet());

        Map<Long, Profile> profilesByUser = profileRepository.findByUserIdIn(autoreIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        Map<Long, Long> likeCounts = postLikeRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostLikeRepository.LikeCount::getPostId,
                        PostLikeRepository.LikeCount::getTotal));
        Set<Long> likedByMe = postLikeRepository.findLikedPostIds(currentUserId, postIds);

        return posts.stream()
                .map(post -> postMapper.toDto(post,
                        profilesByUser.get(post.getAutore().getId()),
                        likeCounts.getOrDefault(post.getId(), 0L),
                        likedByMe.contains(post.getId())))
                .toList();
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
