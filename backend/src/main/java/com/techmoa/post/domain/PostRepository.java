package com.techmoa.post.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT p
            FROM Post p
            JOIN FETCH p.source s
            WHERE (
                :cursorId IS NULL
                OR p.publishedAt < :cursorPublishedAt
                OR (p.publishedAt = :cursorPublishedAt AND p.id < :cursorId)
              )
              AND (
                :tagName IS NULL
                OR EXISTS (
                    SELECT t.id
                    FROM p.tags t
                    WHERE t.name = :tagName
                )
              )
              AND (
                :q IS NULL
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR LOWER(COALESCE(p.summary, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
              )
            ORDER BY p.publishedAt DESC, p.id DESC
            """)
    List<Post> findFeed(
            @Param("cursorId") Long cursorId,
            @Param("cursorPublishedAt") LocalDateTime cursorPublishedAt,
            @Param("tagName") String tagName,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM Post p
            JOIN FETCH p.source s
            WHERE (
                :cursorId IS NULL
                OR p.publishedAt < :cursorPublishedAt
                OR (p.publishedAt = :cursorPublishedAt AND p.id < :cursorId)
              )
              AND s.id IN :sourceIds
              AND (
                :tagName IS NULL
                OR EXISTS (
                    SELECT t.id
                    FROM p.tags t
                    WHERE t.name = :tagName
                )
              )
              AND (
                :q IS NULL
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR LOWER(COALESCE(p.summary, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
              )
            ORDER BY p.publishedAt DESC, p.id DESC
            """)
    List<Post> findFeedBySourceIds(
            @Param("cursorId") Long cursorId,
            @Param("cursorPublishedAt") LocalDateTime cursorPublishedAt,
            @Param("sourceIds") List<Long> sourceIds,
            @Param("tagName") String tagName,
            @Param("q") String q,
            Pageable pageable
    );

    Optional<Post> findByCanonicalUrl(String canonicalUrl);

    @Query("""
            SELECT DISTINCT p
            FROM Post p
            JOIN FETCH p.source s
            LEFT JOIN FETCH p.tags t
            WHERE p.id = :id
            """)
    Optional<Post> findDetailById(@Param("id") Long id);
}
