package com.pickeat.pickeatbackend.domain.member.repository;

import com.pickeat.pickeatbackend.domain.member.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 삭제된 행 수를 돌려준다. 같은 토큰으로 동시에 재발급을 요청하면 한 요청만 1을 받는다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.member.id = :memberId AND t.expiresAt <= :now")
    int deleteExpiredByMemberId(@Param("memberId") Long memberId, @Param("now") Instant now);
}
