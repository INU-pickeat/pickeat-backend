package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record RestaurantNavigationLinksResponse(String naverMapUrl, String kakaoMapUrl) {

    // 네이버·카카오 둘 다 자체 장소 ID가 없어 이름 검색에 의존한다. 동명 지점 오매칭을 줄이려고
    // 좌표를 지도 중심/핀 좌표로 함께 넘긴다: Kakao는 "이름,위도,경도" 핀 공유 링크를 공식 지원하고,
    // 네이버는 검색 결과와 별개로 지도 중심(c 파라미터)을 좌표로 고정한다.
    public static RestaurantNavigationLinksResponse from(Restaurant restaurant) {
        String name = encodePathSegment(restaurant.getName());
        double lat = restaurant.getLatitude();
        double lng = restaurant.getLongitude();

        String kakaoMapUrl = "https://map.kakao.com/link/map/%s,%s,%s".formatted(name, lat, lng);
        String naverMapUrl = "https://map.naver.com/p/search/%s?c=%s,%s,15,0,0,0,dh".formatted(name, lng, lat);

        return new RestaurantNavigationLinksResponse(naverMapUrl, kakaoMapUrl);
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
