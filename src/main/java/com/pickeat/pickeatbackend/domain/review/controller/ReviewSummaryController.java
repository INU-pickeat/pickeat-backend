package com.pickeat.pickeatbackend.domain.review.controller;

import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse;
import com.pickeat.pickeatbackend.domain.review.service.ReviewSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 식당 상세와 같은 경로 아래에 두어 인증 없이 조회할 수 있다(GET /api/v1/restaurants/** 공개).
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class ReviewSummaryController {

    private final ReviewSummaryService reviewSummaryService;

    @GetMapping("/{restaurantId}/review-summary")
    public ResponseEntity<ReviewSummaryResponse> getSummary(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewSummaryService.getSummary(restaurantId));
    }
}
