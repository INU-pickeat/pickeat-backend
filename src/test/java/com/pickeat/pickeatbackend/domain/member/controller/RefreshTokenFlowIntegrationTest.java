package com.pickeat.pickeatbackend.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefreshTokenFlowIntegrationTest {

    private static final String EMAIL = "refresh@pickeat.com";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void signUp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"password123\",\"nickname\":\"리프레시\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("로그인하면 refresh token을 받고, DB에는 원문 대신 해시만 저장된다")
    void issuesRefreshTokenAndStoresOnlyHash() throws Exception {
        String refreshToken = login();

        String storedHash = jdbcTemplate.queryForObject("""
                SELECT t.token_hash FROM refresh_tokens t JOIN members m ON m.id = t.member_id
                WHERE m.email = ?
                """, String.class, EMAIL);
        assertThat(storedHash).hasSize(64).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("재발급하면 새 토큰 쌍을 받고, 한 번 쓴 refresh token은 다시 쓸 수 없다")
    void rotatesRefreshTokenAndRejectsReuse() throws Exception {
        String oldToken = login();

        String newToken = read(refresh(oldToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()), "$.refreshToken");

        assertThat(newToken).isNotEqualTo(oldToken);
        refresh(oldToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MEMBER_004"));
        refresh(newToken).andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그아웃한 refresh token은 재발급에 쓸 수 없고, 로그아웃은 반복해도 성공한다")
    void revokesRefreshTokenOnLogout() throws Exception {
        String refreshToken = login();

        logout(refreshToken).andExpect(status().isNoContent());
        logout(refreshToken).andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 refresh token은 재발급에 쓸 수 없다")
    void rejectsExpiredRefreshToken() throws Exception {
        String refreshToken = login();
        jdbcTemplate.update("UPDATE refresh_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        entityManager.clear(); // 로그인 때 저장한 엔티티가 테스트 트랜잭션에 남아 옛 만료 시각을 읽지 않도록

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MEMBER_004"));
    }

    @Test
    @DisplayName("refresh token 없이 재발급을 요청하면 입력 검증 오류가 난다")
    void rejectsBlankRefreshToken() throws Exception {
        refresh("").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    private String login() throws Exception {
        return read(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()), "$.refreshToken");
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
