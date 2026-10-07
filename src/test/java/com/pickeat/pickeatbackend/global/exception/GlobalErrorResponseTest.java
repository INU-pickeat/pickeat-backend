package com.pickeat.pickeatbackend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalErrorResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("토큰 없이 인증 API를 호출하면 공통 형식의 401 GLOBAL_002를 반환한다")
    void returnsCommonErrorForMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_002"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("없는 경로는 공통 형식의 404 GLOBAL_003을 반환한다")
    void returnsCommonErrorForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/unknown-path")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GLOBAL_003"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 공통 형식의 405 GLOBAL_004를 반환한다")
    void returnsCommonErrorForUnsupportedMethod() throws Exception {
        mockMvc.perform(put("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("GLOBAL_004"));
    }

    @Test
    @DisplayName("깨진 JSON 본문은 공통 형식의 400 GLOBAL_001을 반환한다")
    void returnsCommonErrorForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 내부 정보 없이 500 GLOBAL_500으로 응답한다")
    void hidesUnexpectedExceptionDetails() {
        ResponseEntity<ErrorResponse> response =
                new GlobalExceptionHandler().handleUnexpected(new IllegalStateException("DB 비밀번호 노출"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("GLOBAL_500");
        assertThat(response.getBody().message()).doesNotContain("DB");
    }
}
