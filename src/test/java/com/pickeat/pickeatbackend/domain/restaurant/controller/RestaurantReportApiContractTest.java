package com.pickeat.pickeatbackend.domain.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantReportResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportReason;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportStatus;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantReportService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
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
class RestaurantReportApiContractTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean RestaurantReportService restaurantReportService;

    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        authorizationHeader = "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID);
    }

    @Test
    @DisplayName("식당 신고는 인증이 필요하다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/10/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("신고하면 201과 PENDING 상태를 돌려준다")
    void createsReport() throws Exception {
        when(restaurantReportService.report(eq(10L), any(), eq(MEMBER_ID))).thenReturn(new RestaurantReportResponse(
                5L, 10L, RestaurantReportReason.CLOSED, RestaurantReportStatus.PENDING,
                Instant.parse("2026-10-08T05:00:00Z")));

        mockMvc.perform(post("/api/v1/restaurants/10/reports")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CLOSED\",\"detail\":\"문 닫았어요\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(5))
                .andExpect(jsonPath("$.restaurantId").value(10))
                .andExpect(jsonPath("$.reason").value("CLOSED"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("reason이 없거나 모르는 값이면 400 GLOBAL_001")
    void rejectsMissingOrUnknownReason() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/10/reports")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detail\":\"이유 없음\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
        mockMvc.perform(post("/api/v1/restaurants/10/reports")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"TOO_EXPENSIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    @Test
    @DisplayName("detail이 300자를 넘으면 400 GLOBAL_001")
    void rejectsTooLongDetail() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/10/reports")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"OTHER\",\"detail\":\"" + "가".repeat(301) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    @Test
    @DisplayName("검토 전 중복 신고는 409 RESTAURANT_003")
    void rejectsDuplicatePendingReport() throws Exception {
        when(restaurantReportService.report(eq(10L), any(), eq(MEMBER_ID)))
                .thenThrow(new BusinessException(RestaurantErrorCode.REPORT_ALREADY_PENDING));

        mockMvc.perform(post("/api/v1/restaurants/10/reports")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESTAURANT_003"));
    }
}
