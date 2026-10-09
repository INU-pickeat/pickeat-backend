package com.pickeat.pickeatbackend.domain.review.entity;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import java.util.List;
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

    @Column(nullable = false, length = 1000)
    private String content;

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

    @Builder
    public Review(Pick pick, String content, FoodCategory foodCategory,
                  CompanionType companionType, ReviewVisibility visibility, List<String> imageUrls) {
        this.pick = pick;
        this.member = pick.getMember();
        this.restaurant = pick.getRestaurant();
        this.content = content.strip();
        this.foodCategory = foodCategory;
        this.companionType = companionType;
        this.visibility = visibility;
        replaceImages(imageUrls);
    }

    // null인 값은 건드리지 않는 부분 수정. 작성 시 등록한 이미지는 변경하지 않는다.
    public void update(String content, FoodCategory foodCategory, CompanionType companionType,
                       ReviewVisibility visibility) {
        if (content != null) {
            this.content = content.strip();
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
