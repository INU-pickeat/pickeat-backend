package com.pickeat.pickeatbackend.domain.review.controller;

import com.pickeat.pickeatbackend.domain.review.dto.FeedResponse;
import com.pickeat.pickeatbackend.domain.review.service.ReviewFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final ReviewFeedService reviewFeedService;

    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reviewFeedService.getFeed(memberId, cursor, size));
    }
}
