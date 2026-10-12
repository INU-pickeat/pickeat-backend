package com.pickeat.pickeatbackend.domain.recommendation.service;

import com.pickeat.pickeatbackend.domain.recommendation.exception.RecommendationErrorCode;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;

// 추천 검색 반경. 식당 밀도가 높은 수도권(서울·인천·경기)과 부산은 500m·1km·2km, 그 외 지역은 1km·3km·5km 중 고른다.
// 고르지 않으면 지역 기본값(수도권·부산 1km, 그 외 5km)을 쓴다. 지역 판정은 서버만 하고, 프론트는 응답의 선택지를 그대로 쓴다.
// ponytail: 위경도 사각형 근사라 경계 인접 지역(춘천 서부·철원, 김해·양산 일부 등)이 수도권·부산으로 잡힐 수 있다.
// 정확도가 필요해지면 행정구역 폴리곤이나 역지오코딩으로 교체한다.
public final class SearchRadius {

    static final List<Integer> DENSE_AREA_OPTIONS = List.of(500, 1000, 2000);
    static final List<Integer> DEFAULT_OPTIONS = List.of(1000, 3000, 5000);
    static final int DENSE_AREA_DEFAULT = 1000;
    static final int DEFAULT_DEFAULT = 5000;

    private SearchRadius() {
    }

    public static List<Integer> options(double latitude, double longitude) {
        return isDenseArea(latitude, longitude) ? DENSE_AREA_OPTIONS : DEFAULT_OPTIONS;
    }

    public static int defaultMeters(double latitude, double longitude) {
        return isDenseArea(latitude, longitude) ? DENSE_AREA_DEFAULT : DEFAULT_DEFAULT;
    }

    // 요청 반경이 없으면 지역 기본값, 있으면 그 지역 선택지 안의 값만 받는다(아니면 400 RECOMMENDATION_003).
    public static int resolve(double latitude, double longitude, Integer requestedMeters) {
        if (requestedMeters == null) {
            return defaultMeters(latitude, longitude);
        }
        if (!options(latitude, longitude).contains(requestedMeters)) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RADIUS);
        }
        return requestedMeters;
    }

    static boolean isDenseArea(double latitude, double longitude) {
        boolean metropolitan = latitude >= 36.89 && latitude <= 38.30 && longitude >= 126.30 && longitude <= 127.70;
        boolean busan = latitude >= 34.88 && latitude <= 35.39 && longitude >= 128.76 && longitude <= 129.31;
        return metropolitan || busan;
    }
}
