package com.pickeat.pickeatbackend.domain.member.service;

import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.entity.RefreshToken;
import com.pickeat.pickeatbackend.domain.member.exception.MemberErrorCode;
import com.pickeat.pickeatbackend.domain.member.repository.RefreshTokenRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// access token(JWT, 30분)과 refresh token(무작위 문자열, 14일)을 발급한다. refresh token은 한 번 쓰면
// 지우고 새로 발급한다(rotation). 로그인한 기기마다 행이 하나씩 생기므로 기기별로 따로 로그아웃된다.
// ponytail: 탈취된 토큰의 재사용 감지(토큰 계열 전체 폐기)는 없다. 필요해지면 family_id 컬럼을 추가한다.
@Service
public class AuthTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final Duration refreshTokenValidity;

    public AuthTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenValidity = Duration.ofMillis(refreshTokenValidityMs);
    }

    @Transactional
    public LoginResponse issue(Member member) {
        Instant now = Instant.now();
        refreshTokenRepository.deleteExpiredByMemberId(member.getId(), now);

        String refreshToken = generateToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .member(member)
                .tokenHash(hash(refreshToken))
                .expiresAt(now.plus(refreshTokenValidity))
                .build());

        return new LoginResponse(jwtTokenProvider.createAccessToken(member.getId()), refreshToken);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        String tokenHash = hash(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_REFRESH_TOKEN));
        Member member = stored.getMember();

        // 조회와 삭제 사이에 같은 토큰으로 온 다른 요청이 먼저 지웠다면 0이 나온다.
        if (refreshTokenRepository.deleteByTokenHash(tokenHash) == 0 || stored.isExpired(Instant.now())) {
            throw new BusinessException(MemberErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issue(member);
    }

    // 이미 없거나 만료된 토큰이어도 성공으로 본다. 로그아웃은 여러 번 불려도 결과가 같다.
    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.deleteByTokenHash(hash(refreshToken));
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
