package com.pickeat.pickeatbackend.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.ReviewFixtures;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewTest {

    private final Pick pick = ReviewFixtures.pick(
            30L, ReviewFixtures.member(1L, "작성자"), ReviewFixtures.restaurant(10L, "테스트 식당"));

    @Test
    @DisplayName("후기는 Pick의 작성자와 식당을 그대로 물려받고 이미지를 0번부터 순서대로 저장한다")
    void inheritsMemberAndRestaurantFromPick() {
        Review review = ReviewFixtures.review(
                1L, pick, ReviewVisibility.PUBLIC, "  맛있어요  ", List.of("https://img/a.jpg", "https://img/b.jpg"));

        assertThat(review.getMember().getId()).isEqualTo(1L);
        assertThat(review.getRestaurant().getId()).isEqualTo(10L);
        assertThat(review.getContent()).isEqualTo("맛있어요");
        assertThat(review.getImageUrls()).containsExactly("https://img/a.jpg", "https://img/b.jpg");
        assertThat(review.getImages()).extracting(ReviewImage::getDisplayOrder).containsExactly(0, 1);
        assertThat(review.isPublic()).isTrue();
        assertThat(review.isWrittenBy(1L)).isTrue();
        assertThat(review.isWrittenBy(2L)).isFalse();
    }

    @Test
    @DisplayName("부분 수정은 null인 값을 건드리지 않는다")
    void keepsUnsetFieldsOnPartialUpdate() {
        Review review = ReviewFixtures.review(1L, pick, ReviewVisibility.PUBLIC, "맛있어요", List.of("https://img/a.jpg"));

        review.update(null, null, CompanionType.FAMILY, ReviewVisibility.PRIVATE, null, null);

        assertThat(review.getContent()).isEqualTo("맛있어요");
        assertThat(review.getFoodCategory()).isEqualTo(FoodCategory.KOREAN);
        assertThat(review.getCompanionType()).isEqualTo(CompanionType.FAMILY);
        assertThat(review.isPublic()).isFalse();
        assertThat(review.getImageUrls()).containsExactly("https://img/a.jpg");
        assertThat(review.getSortedKeywords()).containsExactly(ReviewKeyword.TASTY);
        assertThat(review.getSpendPerPerson()).isNull();
    }

    @Test
    @DisplayName("빈 문자열·공백 본문은 본문 없음(null)으로 저장한다")
    void storesBlankContentAsNull() {
        assertThat(ReviewFixtures.review(1L, pick, ReviewVisibility.PUBLIC, null, List.of()).getContent()).isNull();
        assertThat(ReviewFixtures.review(1L, pick, ReviewVisibility.PUBLIC, "   ", List.of()).getContent()).isNull();
    }

    @Test
    @DisplayName("키워드는 enum 선언 순서로 돌려준다")
    void returnsKeywordsInDeclarationOrder() {
        Review review = ReviewFixtures.review(1L, pick, ReviewVisibility.PUBLIC, null, List.of(),
                List.of(ReviewKeyword.DOG_FRIENDLY, ReviewKeyword.TASTY, ReviewKeyword.CLEAN));

        assertThat(review.getSortedKeywords())
                .containsExactly(ReviewKeyword.TASTY, ReviewKeyword.CLEAN, ReviewKeyword.DOG_FRIENDLY);
    }

    @Test
    @DisplayName("수정 시 키워드는 통째로 교체하고 빈 문자열 본문은 본문을 지운다")
    void replacesKeywordsAndClearsContentOnUpdate() {
        Review review = ReviewFixtures.review(1L, pick, ReviewVisibility.PUBLIC, "맛있어요", List.of(),
                List.of(ReviewKeyword.TASTY, ReviewKeyword.CLEAN));

        review.update("", null, null, null, Set.of(ReviewKeyword.KIND_SERVICE), SpendPerPerson.OVER_30000);

        assertThat(review.getContent()).isNull();
        assertThat(review.getSortedKeywords()).containsExactly(ReviewKeyword.KIND_SERVICE);
        assertThat(review.getSpendPerPerson()).isEqualTo(SpendPerPerson.OVER_30000);
    }
}
