package com.pickeat.pickeatbackend.domain.review.repository;

import com.pickeat.pickeatbackend.domain.review.entity.ReviewImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    // Pick마다 후기의 첫 번째(0번) 이미지. Pick 캘린더의 날짜 대표 이미지에 쓴다.
    @Query("""
            SELECT new com.pickeat.pickeatbackend.domain.review.repository.PickImage(i.review.pick.id, i.imageUrl)
            FROM ReviewImage i
            WHERE i.review.pick.id IN :pickIds
              AND i.displayOrder = 0
            """)
    List<PickImage> findFirstImagesByPickIds(@Param("pickIds") Collection<Long> pickIds);
}
