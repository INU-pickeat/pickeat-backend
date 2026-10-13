package com.pickeat.pickeatbackend.domain.review.controller;

import com.pickeat.pickeatbackend.domain.review.dto.NotificationsResponse;
import com.pickeat.pickeatbackend.domain.review.service.ReviewLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
public class MyNotificationController {

    private final ReviewLikeService reviewLikeService;

    @GetMapping
    public ResponseEntity<NotificationsResponse> getMine(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(reviewLikeService.getNotifications(memberId));
    }
}
