package com.ristorandoti.application.service;

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
import com.ristorandoti.application.entity.Post;
import com.ristorandoti.application.entity.PostLike;
import com.ristorandoti.application.entity.Profile;
import com.ristorandoti.application.entity.User;
import com.ristorandoti.application.exception.ResourceNotFoundException;
import com.ristorandoti.application.mapper.PostMapper;
import com.ristorandoti.application.repository.PostLikeRepository;
import com.ristorandoti.application.repository.PostRepository;
import com.ristorandoti.application.repository.ProfileRepository;
import com.ristorandoti.application.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logica di business dei post: feed della community, post di un utente, pubblicazione e like.
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
    private final PostMapper postMapper;

    /**
     * @param currentUserId utente che richiede il feed (per {@code likedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina (limitata a {@value #MAX_PAGE_SIZE})
     * @return una pagina del feed globale, dal post più recente
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostDto> getFeed(Long currentUserId, int page, int size) {
        Page<Post> posts = postRepository.findAll(pageRequest(page, size));
        return PageResponseDto.of(posts, toDtos(posts.getContent(), currentUserId));
    }

    /**
     * @param userId        autore di cui leggere i post
     * @param currentUserId utente che fa la richiesta (per {@code likedByMe})
     * @param page          indice della pagina (da 0)
     * @param size          dimensione della pagina
     * @return una pagina dei post dell'autore, dal più recente
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostDto> getPostsByUser(Long userId, Long currentUserId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Utente " + userId + " non trovato");
        }
        Page<Post> posts = postRepository.findByAutoreId(userId, pageRequest(page, size));
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
     * Mette like a un post. Idempotente: un secondo like dello stesso utente non ha effetto.
     *
     * @param postId post a cui mettere like
     * @param userId utente autenticato
     * @return il post con il conteggio aggiornato
     * @throws ResourceNotFoundException se il post non esiste
     */
    @Transactional
    public PostDto like(Long postId, Long userId) {
        Post post = findPost(postId);
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .user(userRepository.getReferenceById(userId))
                    .build());
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
        Post post = findPost(postId);
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        postLikeRepository.flush();
        return toDto(post, userId);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post " + postId + " non trovato"));
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
