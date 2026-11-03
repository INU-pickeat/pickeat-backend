package com.pickeat.pickeatbackend.domain.review.entity;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 후기는 Pick 하나당 하나다. 작성하면 그 Pick이 REVIEWED가 되어 지도·캘린더에 노출된다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_id", nullable = false, unique = true, updatable = false)
    private Pick pick;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false, updatable = false)
    private Restaurant restaurant;

    // 선택 입력(2026-11-03). 없으면 null이며 빈 문자열·공백은 null로 저장한다.
    @Column(length = 1000)
    private String content;

    // 선택 입력. 작성자가 고른 1인당 지출 구간이다.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SpendPerPerson spendPerPerson;

    // 추천 세션의 값이 아니라 후기 작성 화면에서 사용자가 직접 고른 값이다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FoodCategory foodCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanionType companionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewVisibility visibility;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<ReviewImage> images = new ArrayList<>();

    // "이런 점이 좋았어요" 키워드 1~5개. 개수 검증은 요청 DTO와 ReviewService에서 한다.
    @ElementCollection
    @CollectionTable(name = "review_keywords", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "keyword", nullable = false, length = 30)
    @BatchSize(size = 50)
    private Set<ReviewKeyword> keywords = new LinkedHashSet<>();

    @Builder
    public Review(Pick pick, String content, FoodCategory foodCategory,
                  CompanionType companionType, ReviewVisibility visibility, List<String> imageUrls,
                  Collection<ReviewKeyword> keywords, SpendPerPerson spendPerPerson) {
        this.pick = pick;
        this.member = pick.getMember();
        this.restaurant = pick.getRestaurant();
        this.content = normalizeContent(content);
        this.foodCategory = foodCategory;
        this.companionType = companionType;
        this.visibility = visibility;
        this.spendPerPerson = spendPerPerson;
        replaceImages(imageUrls);
        replaceKeywords(keywords);
    }

    // null인 값은 건드리지 않는 부분 수정. 작성 시 등록한 이미지는 변경하지 않는다.
    // content는 빈 문자열·공백을 보내면 본문을 지운다(null). keywords는 보내면 통째로 교체한다.
    public void update(String content, FoodCategory foodCategory, CompanionType companionType,
                       ReviewVisibility visibility, Collection<ReviewKeyword> keywords,
                       SpendPerPerson spendPerPerson) {
        if (content != null) {
            this.content = normalizeContent(content);
        }
        if (keywords != null) {
            replaceKeywords(keywords);
        }
        if (spendPerPerson != null) {
            this.spendPerPerson = spendPerPerson;
        }
        if (foodCategory != null) {
            this.foodCategory = foodCategory;
        }
        if (companionType != null) {
            this.companionType = companionType;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public boolean isPublic() {
        return visibility == ReviewVisibility.PUBLIC;
    }

    public boolean isWrittenBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public List<String> getImageUrls() {
        return images.stream().map(ReviewImage::getImageUrl).toList();
    }

    // 응답에는 enum 선언 순서로 내려준다.
    public List<ReviewKeyword> getSortedKeywords() {
        return keywords.stream().sorted().toList();
    }

    private static String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.strip();
    }

    private void replaceKeywords(Collection<ReviewKeyword> keywords) {
        this.keywords.clear();
        if (keywords != null) {
            this.keywords.addAll(keywords);
        }
    }

    private void replaceImages(List<String> imageUrls) {
        images.clear();
        if (imageUrls == null) {
            return;
        }
        for (int order = 0; order < imageUrls.size(); order++) {
            images.add(new ReviewImage(this, imageUrls.get(order), order));
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
