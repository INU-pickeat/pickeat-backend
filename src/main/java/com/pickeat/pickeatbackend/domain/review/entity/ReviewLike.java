package com.pickeat.pickeatbackend.domain.review.entity;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 좋아요 행은 ReviewLikeRepository.insertIfAbsent(네이티브 upsert)로만 만든다. created_at은 DB 기본값이 채운다.
@Entity
@Table(name = "review_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, updatable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
