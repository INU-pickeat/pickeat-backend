package com.pickeat.pickeatbackend.domain.review.repository;

import com.pickeat.pickeatbackend.domain.review.entity.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByPickId(Long pickId);

    @EntityGraph(attributePaths = {"restaurant", "pick"})
    Optional<Review> findByIdAndMemberId(Long id, Long memberId);

    @Query("SELECT r FROM Review r JOIN FETCH r.restaurant JOIN FETCH r.member WHERE r.id = :id")
    Optional<Review> findDetailById(@Param("id") Long id);

    // 공개 피드 첫 페이지: 최신순(id DESC).
    @Query("""
            SELECT r FROM Review r JOIN FETCH r.restaurant JOIN FETCH r.member
            WHERE r.visibility = com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility.PUBLIC
            ORDER BY r.id DESC
            """)
    List<Review> findPublicFeed(Pageable pageable);

    // 공개 피드 다음 페이지: cursor(이전 페이지 마지막 reviewId)보다 오래된 후기.
    @Query("""
            SELECT r FROM Review r JOIN FETCH r.restaurant JOIN FETCH r.member
            WHERE r.visibility = com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility.PUBLIC
              AND r.id < :cursor
            ORDER BY r.id DESC
            """)
    List<Review> findPublicFeedBefore(@Param("cursor") Long cursor, Pageable pageable);

    // 식당별 후기 요약은 공개 후기만 집계한다.
    @Query("""
            SELECT new com.pickeat.pickeatbackend.domain.review.repository.ReviewStats(COUNT(r), AVG(r.rating))
            FROM Review r
            WHERE r.restaurant.id = :restaurantId
              AND r.visibility = com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility.PUBLIC
            """)
    ReviewStats findPublicStats(@Param("restaurantId") Long restaurantId);

    // 식당마다 가장 최근 공개 후기 한 건. 대표 한줄평의 원본이다.
    @Query("""
            SELECT r FROM Review r
            WHERE r.id IN (
                SELECT MAX(latest.id) FROM Review latest
                WHERE latest.visibility = com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility.PUBLIC
                  AND latest.restaurant.id IN :restaurantIds
                GROUP BY latest.restaurant.id
            )
            """)
    List<Review> findLatestPublicByRestaurantIds(@Param("restaurantIds") Collection<Long> restaurantIds);
}
