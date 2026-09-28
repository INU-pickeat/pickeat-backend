package com.pickeat.pickeatbackend.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.member.dto.MemberProfileResponse;
import com.pickeat.pickeatbackend.domain.member.dto.UpdateProfileRequest;
import com.pickeat.pickeatbackend.domain.member.service.MemberService;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberApiContractTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockitoBean MemberService memberService;

    private String authorizationHeader;
    private MemberProfileResponse profile;

    @BeforeEach
    void setUp() {
        authorizationHeader = "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID);
        profile = new MemberProfileResponse(
                MEMBER_ID, "test@pickeat.com", "픽잇러", "혼밥 전문", "https://cdn.pickeat.com/p.jpg");
    }

    @Test
    @DisplayName("내 프로필 API는 인증이 필요하다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 프로필 조회 API는 프로필 계약을 반환한다")
    void getsMyProfileWithDocumentedContract() throws Exception {
        when(memberService.getProfile(MEMBER_ID)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/me").header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.email").value("test@pickeat.com"))
                .andExpect(jsonPath("$.nickname").value("픽잇러"))
                .andExpect(jsonPath("$.bio").value("혼밥 전문"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://cdn.pickeat.com/p.jpg"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("내 프로필 수정 API는 수정된 프로필을 반환한다")
    void updatesMyProfileWithDocumentedContract() throws Exception {
        when(memberService.updateProfile(eq(MEMBER_ID), any(UpdateProfileRequest.class))).thenReturn(profile);

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"픽잇러\",\"bio\":\"혼밥 전문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("픽잇러"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nickname\":\"   \"}",
            "{\"nickname\":\"1234567890123456789012345678901\"}",
            "{\"profileImageUrl\":\"javascript:alert(1)\"}"
    })
    @DisplayName("내 프로필 수정 API는 잘못된 입력을 거부한다")
    void rejectsInvalidProfileUpdate(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        verify(memberService, never()).updateProfile(any(), any());
    }
}
