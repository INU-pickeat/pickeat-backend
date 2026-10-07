package com.pickeat.pickeatbackend.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_verifications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailVerification {

    @Id
    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    public void replaceCode(String codeHash, Instant sentAt, Instant expiresAt) {
        this.codeHash = codeHash;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
        this.verifiedAt = null;
        this.failedAttempts = 0;
    }

    public void markVerified(Instant now, Instant signupExpiresAt) {
        this.verifiedAt = now;
        this.expiresAt = signupExpiresAt;
    }

    public void recordFailedAttempt() {
        failedAttempts++;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isVerified(Instant now) {
        return verifiedAt != null && !isExpired(now);
    }
}
