package com.pickeat.pickeatbackend.domain.pick.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.PickMapResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickPeriod;
import com.pickeat.pickeatbackend.domain.pick.dto.PickResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickStatusUpdateRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.RecentPicksResponse;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.pick.repository.RestaurantPickSummary;
import com.pickeat.pickeatbackend.domain.pick.service.PickService;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
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
class PickApiContractTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PICK_ID = 30L;
    private static final Instant SELECTED_AT = Instant.parse("2026-09-21T10:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean PickService pickService;

    private String authorizationHeader;
    private PickResponse selectedResponse;

    @BeforeEach
    void setUp() {
        authorizationHeader = "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID);
        selectedResponse = new PickResponse(PICK_ID, 10L, "테스트 식당", PickStatus.SELECTED, SELECTED_AT, null);
    }

    @Test
    @DisplayName("Pick API는 인증이 필요하다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/picks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pick 생성 API는 201과 생성 결과를 반환한다")
    void createsPickWithDocumentedContract() throws Exception {
        when(pickService.create(any(CreatePickRequest.class), eq(MEMBER_ID))).thenReturn(selectedResponse);

        mockMvc.perform(post("/api/v1/picks")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pickId").value(PICK_ID))
                .andExpect(jsonPath("$.restaurantId").value(10))
                .andExpect(jsonPath("$.restaurantName").value("테스트 식당"))
                .andExpect(jsonPath("$.status").value("SELECTED"))
                .andExpect(jsonPath("$.selectedAt").value("2026-09-21T10:00:00Z"))
                .andExpect(jsonPath("$.visitedAt").doesNotExist());
    }

    @Test
    @DisplayName("Pick 상태 변경 API는 변경된 계약을 반환한다")
    void updatesPickStatusWithDocumentedContract() throws Exception {
        PickResponse reviewed = new PickResponse(
                PICK_ID, 10L, "테스트 식당", PickStatus.REVIEWED, SELECTED_AT,
                Instant.parse("2026-09-21T11:00:00Z"));
        when(pickService.updateStatus(eq(PICK_ID), any(PickStatusUpdateRequest.class), eq(MEMBER_ID)))
                .thenReturn(reviewed);

        mockMvc.perform(patch("/api/v1/picks/{pickId}", PICK_ID)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"))
                .andExpect(jsonPath("$.visitedAt").value("2026-09-21T11:00:00Z"));
    }

    @Test
    @DisplayName("내 최근 Pick 목록 API는 식당별 집계 계약을 반환한다")
    void getsMyRecentPicksWithRestaurantSummaryContract() throws Exception {
        RecentPicksResponse response = new RecentPicksResponse(List.of(
                new RecentPicksResponse.Item(10L, "테스트 식당", 3L, SELECTED_AT)));
        when(pickService.getMyPicks(MEMBER_ID, PickPeriod.WEEK)).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/picks")
                        .header("Authorization", authorizationHeader)
                        .param("period", "week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurants.length()").value(1))
                .andExpect(jsonPath("$.restaurants[0].restaurantId").value(10))
                .andExpect(jsonPath("$.restaurants[0].pickCount").value(3))
                .andExpect(jsonPath("$.restaurants[0].latestPickedAt").value("2026-09-21T10:00:00Z"));
    }

    @Test
    @DisplayName("내 Pick 지도 API는 좌표와 동행 유형 계약을 반환한다")
    void getsMyPickMapWithCoordinateContract() throws Exception {
        PickMapResponse response = new PickMapResponse(List.of(
                new PickMapResponse.Item(PICK_ID, 10L, "테스트 식당", 37.5, 127.0, PickStatus.REVIEWED, CompanionType.SOLO)));
        when(pickService.getMyPickMap(MEMBER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/picks/map")
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.picks[0].pickId").value(PICK_ID))
                .andExpect(jsonPath("$.picks[0].latitude").value(37.5))
                .andExpect(jsonPath("$.picks[0].longitude").value(127.0))
                .andExpect(jsonPath("$.picks[0].companionType").value("SOLO"));
    }

    @Test
    @DisplayName("필수 값이 없는 Pick 생성 요청은 공통 입력 오류를 반환한다")
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/picks")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(pickService, never()).create(any(), any());
    }

    @Test
    @DisplayName("잘못된 기간 조건은 공통 입력 오류를 반환한다")
    void rejectsInvalidPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/me/picks")
                        .header("Authorization", authorizationHeader)
                        .param("period", "year"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(pickService, never()).getMyPicks(any(), any());
    }

    private String validCreateRequest() {
        return """
                {
                  "recommendationSessionId": 20,
                  "restaurantId": 10
                }
                """;
    }
}
