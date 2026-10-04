package com.pickeat.pickeatbackend.domain.review.repository;

import com.pickeat.pickeatbackend.domain.review.entity.ReviewLike;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    long countByReviewId(Long reviewId);

    boolean existsByReviewIdAndMemberId(Long reviewId, Long memberId);

    // 같은 사용자가 동시에 두 번 눌러도 한 행만 남도록 DB 유니크 제약에 맡긴다.
    @Modifying
    @Query(value = """
            INSERT INTO review_likes (review_id, member_id)
            VALUES (:reviewId, :memberId)
            ON CONFLICT (review_id, member_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("reviewId") Long reviewId, @Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM ReviewLike l WHERE l.review.id = :reviewId AND l.member.id = :memberId")
    int deleteByReviewAndMember(@Param("reviewId") Long reviewId, @Param("memberId") Long memberId);

    @Query("""
            SELECT new com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeCount(l.review.id, COUNT(l))
            FROM ReviewLike l
            WHERE l.review.id IN :reviewIds
            GROUP BY l.review.id
            """)
    List<ReviewLikeCount> countByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);

    @Query("SELECT l.review.id FROM ReviewLike l WHERE l.member.id = :memberId AND l.review.id IN :reviewIds")
    List<Long> findLikedReviewIds(@Param("memberId") Long memberId, @Param("reviewIds") Collection<Long> reviewIds);
}
