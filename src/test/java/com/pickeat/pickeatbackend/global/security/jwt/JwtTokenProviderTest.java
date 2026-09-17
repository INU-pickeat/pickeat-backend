package com.pickeat.pickeatbackend.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("test-secret-key-please-make-it-long-enough-for-hs256", 1_000L * 60);

    @Test
    void 발급한_토큰에서_유저_ID를_그대로_추출한다() {
        String token = jwtTokenProvider.createAccessToken(42L);

        assertThat(jwtTokenProvider.isValid(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void 조작된_토큰은_유효하지_않다() {
        String token = jwtTokenProvider.createAccessToken(42L);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtTokenProvider.isValid(tampered)).isFalse();
    }
}
