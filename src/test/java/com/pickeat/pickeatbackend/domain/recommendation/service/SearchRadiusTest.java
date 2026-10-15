package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pickeat.pickeatbackend.domain.recommendation.exception.RecommendationErrorCode;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchRadiusTest {

    private static final double SEOUL_LAT = 37.5665;
    private static final double SEOUL_LNG = 126.9780;
    private static final double DAEJEON_LAT = 36.3504;
    private static final double DAEJEON_LNG = 127.3845;

    @Test
    @DisplayName("수도권·부산은 500m·1km·2km, 그 외 지역은 1km·3km·5km를 고를 수 있다")
    void optionsByRegion() {
        assertThat(SearchRadius.options(SEOUL_LAT, SEOUL_LNG)).containsExactly(500, 1000, 2000);
        assertThat(SearchRadius.options(35.1796, 129.0756)).containsExactly(500, 1000, 2000); // 부산
        assertThat(SearchRadius.options(DAEJEON_LAT, DAEJEON_LNG)).containsExactly(1000, 3000, 5000);
    }

    @Test
    @DisplayName("반경을 고르지 않으면 수도권·부산 500m, 그 외 5km")
    void defaultsByRegion() {
        assertThat(SearchRadius.resolve(SEOUL_LAT, SEOUL_LNG, null)).isEqualTo(500);
        assertThat(SearchRadius.resolve(DAEJEON_LAT, DAEJEON_LNG, null)).isEqualTo(5000);
    }

    @Test
    @DisplayName("지역 선택지 안의 반경은 그대로 쓴다")
    void acceptsRegionalOption() {
        assertThat(SearchRadius.resolve(SEOUL_LAT, SEOUL_LNG, 500)).isEqualTo(500);
        assertThat(SearchRadius.resolve(SEOUL_LAT, SEOUL_LNG, 2000)).isEqualTo(2000);
        assertThat(SearchRadius.resolve(DAEJEON_LAT, DAEJEON_LNG, 3000)).isEqualTo(3000);
    }

    @Test
    @DisplayName("지역 선택지 밖의 반경은 RECOMMENDATION_003으로 거부한다")
    void rejectsValueOutsideRegionalOptions() {
        assertThatThrownBy(() -> SearchRadius.resolve(SEOUL_LAT, SEOUL_LNG, 5000))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RecommendationErrorCode.INVALID_RADIUS.getMessage());
        assertThatThrownBy(() -> SearchRadius.resolve(DAEJEON_LAT, DAEJEON_LNG, 500))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SearchRadius.resolve(SEOUL_LAT, SEOUL_LNG, 1500))
                .isInstanceOf(BusinessException.class);
    }
}
