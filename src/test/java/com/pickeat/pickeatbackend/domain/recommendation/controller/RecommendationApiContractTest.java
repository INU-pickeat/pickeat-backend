package com.pickeat.pickeatbackend.domain.recommendation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationResponse;
import com.pickeat.pickeatbackend.domain.recommendation.service.RecommendationService;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
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
class RecommendationApiContractTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SESSION_ID = 44L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RecommendationService recommendationService;

    private String authorizationHeader;
    private RecommendationResponse response;

    @BeforeEach
    void setUp() {
        authorizationHeader = "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID);
        response = new RecommendationResponse(SESSION_ID, List.of(
                new RecommendationResponse.Item(
                        10L, "테스트 식당", FoodCategory.KOREAN, BigDecimal.valueOf(4.5), 320.5, 1, 0.914)
        ));
    }

    @Test
    @DisplayName("추천 생성 API는 인증이 필요하다")
    void requiresAuthenticationToCreateRecommendation() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("추천 생성 API는 201과 세션 및 후보 계약을 반환한다")
    void createsRecommendationWithDocumentedResponseContract() throws Exception {
        when(recommendationService.recommend(any(RecommendationRequest.class), eq(MEMBER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/recommendations")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].restaurantId").value(10))
                .andExpect(jsonPath("$.items[0].name").value("테스트 식당"))
                .andExpect(jsonPath("$.items[0].foodCategory").value("KOREAN"))
                .andExpect(jsonPath("$.items[0].externalRating").value(4.5))
                .andExpect(jsonPath("$.items[0].distanceMeters").value(320.5))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].score").value(0.914));

        verify(recommendationService).recommend(any(RecommendationRequest.class), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("추천 세션 조회 API는 저장된 응답 계약을 반환한다")
    void getsRecommendationSessionWithDocumentedResponseContract() throws Exception {
        when(recommendationService.getSession(SESSION_ID, MEMBER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/recommendations/{sessionId}", SESSION_ID)
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.items[0].restaurantId").value(10))
                .andExpect(jsonPath("$.items[0].rank").value(1));

        verify(recommendationService).getSession(SESSION_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("잘못된 추천 요청은 공통 입력 오류 계약을 반환한다")
    void rejectsInvalidRequestWithGlobalErrorContract() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodCategories": [],
                                  "companionType": null,
                                  "latitude": 91.0,
                                  "longitude": 127.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(recommendationService, never()).recommend(any(), any());
    }

    @Test
    @DisplayName("가격대를 포함한 추천 요청도 201을 반환한다")
    void createsRecommendationWithPriceRange() throws Exception {
        when(recommendationService.recommend(any(RecommendationRequest.class), eq(MEMBER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/recommendations")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodCategories": ["KOREAN"],
                                  "companionType": "DATE",
                                  "priceRange": {"min": 10000, "max": 30000},
                                  "latitude": 37.5,
                                  "longitude": 127.0
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("가격 하한이 상한보다 크면 공통 입력 오류 계약을 반환한다")
    void rejectsPriceRangeMinGreaterThanMaxWithGlobalErrorContract() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodCategories": ["KOREAN"],
                                  "companionType": "DATE",
                                  "priceRange": {"min": 30000, "max": 10000},
                                  "latitude": 37.5,
                                  "longitude": 127.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(recommendationService, never()).recommend(any(), any());
    }

    private String validRequest() {
        return """
                {
                  "foodCategories": ["KOREAN"],
                  "companionType": "DATE",
                  "latitude": 37.5,
                  "longitude": 127.0
                }
                """;
    }
}
