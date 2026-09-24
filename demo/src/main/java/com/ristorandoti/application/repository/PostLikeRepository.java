package com.ristorandoti.application.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ristorandoti.application.entity.PostLike;

/**
 * Repository Spring Data JPA per l'entità {@link PostLike}.
 *
 * <p>Le query "in blocco" ({@link #countByPostIds}, {@link #findLikedPostIds}) servono al feed:
 * per una pagina di N post bastano due query in tutto, non due per ogni post.
 * Non vanno chiamate con una collezione vuota (Oracle non accetta {@code IN ()}).</p>
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /** Numero di like di un post (proiezione di {@link #countByPostIds}). */
    interface LikeCount {
        Long getPostId();

        Long getTotal();
    }

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    /**
     * @param postIds id dei post (non vuota)
     * @return il numero di like per ciascun post; i post senza like non compaiono
     */
    @Query("select l.post.id as postId, count(l) as total from PostLike l "
            + "where l.post.id in :postIds group by l.post.id")
    List<LikeCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    /**
     * @param userId  utente di cui verificare i like
     * @param postIds id dei post (non vuota)
     * @return gli id, tra quelli indicati, dei post a cui l'utente ha messo like
     */
    @Query("select l.post.id from PostLike l where l.user.id = :userId and l.post.id in :postIds")
    Set<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
