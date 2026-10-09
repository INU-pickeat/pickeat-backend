package com.pickeat.pickeatbackend.domain.review;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import java.time.Instant;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

// 후기 단위 테스트에서 공통으로 쓰는 엔티티 조립 도우미.
public final class ReviewFixtures {

    private ReviewFixtures() {
    }

    public static Member member(Long id, String nickname) {
        Member member = Member.builder().email("user" + id + "@pickeat.com").password("password").nickname(nickname).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static Restaurant restaurant(Long id, String name) {
        Restaurant restaurant = Restaurant.builder().name(name).latitude(37.5).longitude(127.0).build();
        ReflectionTestUtils.setField(restaurant, "id", id);
        return restaurant;
    }

    public static Pick pick(Long id, Member member, Restaurant restaurant) {
        Pick pick = Pick.builder().member(member).restaurant(restaurant).companionType(CompanionType.DATE).build();
        ReflectionTestUtils.setField(pick, "id", id);
        return pick;
    }

    public static Review review(Long id, Pick pick, ReviewVisibility visibility, String content, List<String> imageUrls) {
        Review review = Review.builder()
                .pick(pick)
                .content(content)
                .foodCategory(FoodCategory.KOREAN)
                .companionType(CompanionType.SOLO)
                .visibility(visibility)
                .imageUrls(imageUrls)
                .build();
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "createdAt", Instant.parse("2026-10-04T03:00:00Z"));
        ReflectionTestUtils.setField(review, "updatedAt", Instant.parse("2026-10-04T03:00:00Z"));
        return review;
    }
}
