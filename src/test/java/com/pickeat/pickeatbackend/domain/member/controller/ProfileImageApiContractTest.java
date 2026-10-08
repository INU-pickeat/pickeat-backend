package com.pickeat.pickeatbackend.domain.member.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage;
import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage.PresignedUpload;
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
class ProfileImageApiContractTest {

    private static final Long MEMBER_ID = 1L;
    private static final String PATH = "/api/v1/me/profile-image/upload-url";

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean ReviewImageStorage imageStorage;

    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        authorizationHeader = "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 URL 발급은 인증이 필요하다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"contentType\":\"image/jpeg\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("업로드 URL과 저장될 이미지 주소를 돌려준다")
    void returnsPresignedUpload() throws Exception {
        when(imageStorage.createProfileUpload(MEMBER_ID, "image/png")).thenReturn(new PresignedUpload(
                "https://bucket.s3.amazonaws.com/profiles/1/a.png?X-Amz-Signature=x",
                "https://img.pickeat.kr/profiles/1/a.png",
                Instant.parse("2026-10-08T05:10:00Z")));

        mockMvc.perform(post(PATH)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://bucket.s3.amazonaws.com/profiles/1/a.png?X-Amz-Signature=x"))
                .andExpect(jsonPath("$.imageUrl").value("https://img.pickeat.kr/profiles/1/a.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("contentType이 비어 있으면 400 GLOBAL_001")
    void rejectsBlankContentType() throws Exception {
        mockMvc.perform(post(PATH)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    @Test
    @DisplayName("지원하지 않는 형식은 400 REVIEW_005, 저장소 미설정은 503 REVIEW_006")
    void passesStorageErrorsThrough() throws Exception {
        when(imageStorage.createProfileUpload(MEMBER_ID, "image/gif"))
                .thenThrow(new BusinessException(ReviewErrorCode.UNSUPPORTED_IMAGE_TYPE));
        when(imageStorage.createProfileUpload(MEMBER_ID, "image/jpeg"))
                .thenThrow(new BusinessException(ReviewErrorCode.IMAGE_STORAGE_NOT_CONFIGURED));

        mockMvc.perform(post(PATH)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/gif\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REVIEW_005"));
        mockMvc.perform(post(PATH)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("REVIEW_006"));
    }
}
