package com.pickeat.pickeatbackend.domain.review.controller;

import com.pickeat.pickeatbackend.domain.review.dto.CreateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadRequest;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewLikeResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewResponse;
import com.pickeat.pickeatbackend.domain.review.dto.UpdateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.service.ReviewLikeService;
import com.pickeat.pickeatbackend.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request, memberId));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> get(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(reviewService.get(reviewId, memberId));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(reviewService.update(reviewId, request, memberId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long memberId
    ) {
        reviewService.delete(reviewId, memberId);
        return ResponseEntity.noContent().build();
    }

    // 후기 이미지를 S3에 직접 올릴 임시 URL을 발급한다. 올린 뒤 받은 imageUrl을 후기 작성 요청에 넣는다.
    @PostMapping("/images/upload-urls")
    public ResponseEntity<ReviewImageUploadResponse> createImageUploads(
            @Valid @RequestBody ReviewImageUploadRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(reviewService.createImageUploads(request, memberId));
    }

    @PostMapping("/{reviewId}/likes")
    public ResponseEntity<ReviewLikeResponse> like(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(reviewLikeService.like(reviewId, memberId));
    }

    @DeleteMapping("/{reviewId}/likes")
    public ResponseEntity<ReviewLikeResponse> unlike(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(reviewLikeService.unlike(reviewId, memberId));
    }
}
