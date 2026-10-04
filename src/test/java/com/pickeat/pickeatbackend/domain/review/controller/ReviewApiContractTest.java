package com.pickeat.pickeatbackend.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.dto.CreateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.dto.FeedResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadRequest;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewLikeResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse;
import com.pickeat.pickeatbackend.domain.review.dto.UpdateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.domain.review.service.ReviewFeedService;
import com.pickeat.pickeatbackend.domain.review.service.ReviewLikeService;
import com.pickeat.pickeatbackend.domain.review.service.ReviewService;
import com.pickeat.pickeatbackend.domain.review.service.ReviewSummaryService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewApiContractTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long REVIEW_ID = 100L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-04T03:00:00Z");
    private static final String IMAGE_URL = "https://img.pickeat.kr/reviews/1/a.jpg";

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean ReviewService reviewService;
    @MockitoBean ReviewLikeService reviewLikeService;
    @MockitoBean ReviewFeedService reviewFeedService;
    @MockitoBean ReviewSummaryService reviewSummaryService;

    private String authorizationHeader;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        authorizationHeader = "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID);
        reviewResponse = new ReviewResponse(
                REVIEW_ID, 30L, 10L, "테스트 식당", 4, "양고기가 부드러워요", FoodCategory.JAPANESE,
                CompanionType.FAMILY, ReviewVisibility.PUBLIC, List.of(IMAGE_URL), 0, false, CREATED_AT, CREATED_AT);
    }

    @Test
    @DisplayName("후기 API는 인증이 필요하다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/feed")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/likes", REVIEW_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("후기 작성 API는 201과 작성된 후기를 반환한다")
    void createsReviewWithDocumentedContract() throws Exception {
        when(reviewService.create(any(CreateReviewRequest.class), eq(MEMBER_ID))).thenReturn(reviewResponse);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.pickId").value(30))
                .andExpect(jsonPath("$.restaurantId").value(10))
                .andExpect(jsonPath("$.restaurantName").value("테스트 식당"))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.content").value("양고기가 부드러워요"))
                .andExpect(jsonPath("$.foodCategory").value("JAPANESE"))
                .andExpect(jsonPath("$.companionType").value("FAMILY"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.imageUrls[0]").value(IMAGE_URL))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false))
                .andExpect(jsonPath("$.createdAt").value("2026-10-04T03:00:00Z"));
    }

    @Test
    @DisplayName("별점 범위를 벗어나거나 필수 값이 없는 후기 작성 요청은 공통 입력 오류를 반환한다")
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pickId": 30,
                                  "rating": 6,
                                  "content": "맛있어요",
                                  "foodCategory": "JAPANESE",
                                  "companionType": "FAMILY",
                                  "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickId\": 30, \"rating\": 4, \"content\": \"맛있어요\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(reviewService, never()).create(any(), any());
    }

    @Test
    @DisplayName("이미 후기가 있는 Pick이면 REVIEW_002 충돌을 반환한다")
    void returnsConflictForDuplicateReview() throws Exception {
        when(reviewService.create(any(CreateReviewRequest.class), eq(MEMBER_ID)))
                .thenThrow(new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_002"));
    }

    @Test
    @DisplayName("후기 조회·수정·삭제 API는 문서화된 계약을 따른다")
    void getsUpdatesAndDeletesReview() throws Exception {
        when(reviewService.get(REVIEW_ID, MEMBER_ID)).thenReturn(reviewResponse);
        when(reviewService.update(eq(REVIEW_ID), any(UpdateReviewRequest.class), eq(MEMBER_ID)))
                .thenReturn(reviewResponse);

        mockMvc.perform(get("/api/v1/reviews/{reviewId}", REVIEW_ID).header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"visibility\": \"PRIVATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID));

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID).header("Authorization", authorizationHeader))
                .andExpect(status().isNoContent());
        verify(reviewService).delete(REVIEW_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("공백뿐인 내용으로는 후기를 수정할 수 없다")
    void rejectsBlankContentOnUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/reviews/{reviewId}", REVIEW_ID)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(reviewService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("이미지 업로드 URL 발급 API는 업로드 주소와 저장될 이미지 주소를 반환한다")
    void createsImageUploadUrls() throws Exception {
        ReviewImageUploadResponse response = new ReviewImageUploadResponse(List.of(
                new ReviewImageUploadResponse.Upload(
                        "https://s3.example.com/upload", IMAGE_URL, "image/jpeg", Instant.parse("2026-10-04T03:10:00Z"))));
        when(reviewService.createImageUploads(any(ReviewImageUploadRequest.class), eq(MEMBER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/images/upload-urls")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentTypes\": [\"image/jpeg\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploads[0].uploadUrl").value("https://s3.example.com/upload"))
                .andExpect(jsonPath("$.uploads[0].imageUrl").value(IMAGE_URL))
                .andExpect(jsonPath("$.uploads[0].contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.uploads[0].expiresAt").value("2026-10-04T03:10:00Z"));
    }

    @Test
    @DisplayName("이미지 저장소가 준비되지 않았으면 REVIEW_006과 503을 반환한다")
    void returnsServiceUnavailableWhenStorageIsNotConfigured() throws Exception {
        when(reviewService.createImageUploads(any(ReviewImageUploadRequest.class), eq(MEMBER_ID)))
                .thenThrow(new BusinessException(ReviewErrorCode.IMAGE_STORAGE_NOT_CONFIGURED));

        mockMvc.perform(post("/api/v1/reviews/images/upload-urls")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentTypes\": [\"image/jpeg\"]}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("REVIEW_006"));
    }

    @Test
    @DisplayName("공개 피드 API는 후기 목록과 다음 커서를 반환한다")
    void getsPublicFeed() throws Exception {
        FeedResponse response = new FeedResponse(List.of(new FeedResponse.Item(
                REVIEW_ID, 10L, "테스트 식당", "작성자", null, 4, "양고기가 부드러워요", FoodCategory.JAPANESE,
                CompanionType.FAMILY, List.of(IMAGE_URL), 12, true, CREATED_AT)), 100L);
        when(reviewFeedService.getFeed(MEMBER_ID, 150L, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", authorizationHeader)
                        .param("cursor", "150")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.items[0].restaurantId").value(10))
                .andExpect(jsonPath("$.items[0].restaurantName").value("테스트 식당"))
                .andExpect(jsonPath("$.items[0].authorNickname").value("작성자"))
                .andExpect(jsonPath("$.items[0].content").value("양고기가 부드러워요"))
                .andExpect(jsonPath("$.items[0].imageUrls[0]").value(IMAGE_URL))
                .andExpect(jsonPath("$.items[0].likeCount").value(12))
                .andExpect(jsonPath("$.items[0].likedByMe").value(true))
                .andExpect(jsonPath("$.nextCursor").value(100));
    }

    @Test
    @DisplayName("공개 피드는 커서 없이 호출하면 첫 페이지를 기본 크기 20으로 조회한다")
    void getsFirstFeedPageWithDefaults() throws Exception {
        when(reviewFeedService.getFeed(MEMBER_ID, null, 20)).thenReturn(new FeedResponse(List.of(), null));

        mockMvc.perform(get("/api/v1/feed").header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("좋아요·좋아요 취소 API는 현재 상태와 좋아요 수를 반환한다")
    void likesAndUnlikesReview() throws Exception {
        when(reviewLikeService.like(REVIEW_ID, MEMBER_ID)).thenReturn(new ReviewLikeResponse(REVIEW_ID, true, 13));
        when(reviewLikeService.unlike(REVIEW_ID, MEMBER_ID)).thenReturn(new ReviewLikeResponse(REVIEW_ID, false, 12));

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/likes", REVIEW_ID).header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(13));

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}/likes", REVIEW_ID)
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(12));
    }

    @Test
    @DisplayName("비공개 후기에 좋아요를 누르면 REVIEW_003 충돌을 반환한다")
    void rejectsLikeOnPrivateReview() throws Exception {
        when(reviewLikeService.like(REVIEW_ID, MEMBER_ID))
                .thenThrow(new BusinessException(ReviewErrorCode.REVIEW_NOT_PUBLIC));

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/likes", REVIEW_ID).header("Authorization", authorizationHeader))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_003"));
    }

    @Test
    @DisplayName("식당별 후기 요약 API는 인증 없이 후기 수·평균 별점·한줄평을 반환한다")
    void getsReviewSummaryWithoutAuthentication() throws Exception {
        when(reviewSummaryService.getSummary(10L))
                .thenReturn(new ReviewSummaryResponse(10L, 3, 4.3, "양고기가 부드러워요"));

        mockMvc.perform(get("/api/v1/restaurants/{restaurantId}/review-summary", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(10))
                .andExpect(jsonPath("$.reviewCount").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.3))
                .andExpect(jsonPath("$.oneLineReview").value("양고기가 부드러워요"));
    }

    private String validCreateRequest() {
        return """
                {
                  "pickId": 30,
                  "rating": 4,
                  "content": "양고기가 부드러워요",
                  "foodCategory": "JAPANESE",
                  "companionType": "FAMILY",
                  "visibility": "PUBLIC",
                  "imageUrls": ["https://img.pickeat.kr/reviews/1/a.jpg"]
                }
                """;
    }
}
