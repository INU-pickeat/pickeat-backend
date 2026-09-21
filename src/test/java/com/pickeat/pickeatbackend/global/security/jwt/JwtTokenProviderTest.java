package com.pickeat.pickeatbackend.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("test-secret-key-please-make-it-long-enough-for-hs256", 1_000L * 60);

    @Test
    @DisplayName("발급한 토큰에서 유저 ID를 그대로 추출한다")
    void extractsUserIdFromIssuedToken() {
        String token = jwtTokenProvider.createAccessToken(42L);

        assertThat(jwtTokenProvider.isValid(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("조작된 토큰은 유효하지 않다")
    void rejectsTamperedToken() {
        String token = jwtTokenProvider.createAccessToken(42L);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtTokenProvider.isValid(tampered)).isFalse();
    }
}
