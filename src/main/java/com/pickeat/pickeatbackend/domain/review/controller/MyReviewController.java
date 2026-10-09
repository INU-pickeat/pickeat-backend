package com.pickeat.pickeatbackend.domain.review.controller;

import com.pickeat.pickeatbackend.domain.pick.dto.PickPeriod;
import com.pickeat.pickeatbackend.domain.review.dto.MyReviewsResponse;
import com.pickeat.pickeatbackend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/reviews")
@RequiredArgsConstructor
public class MyReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<MyReviewsResponse> getMine(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String period
    ) {
        return ResponseEntity.ok(reviewService.getMine(memberId, PickPeriod.from(period)));
    }
}
