package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import java.util.List;

// Google 사진을 보여줄 때 함께 표시할 출처. 프론트는 "Google Maps"와 촬영자 이름(프로필 링크)을 사진 근처에 보여준다.
public record RestaurantPhotoAttributionResponse(String provider, List<Author> authors) {

    public static final String GOOGLE_MAPS = "Google Maps";

    public static RestaurantPhotoAttributionResponse from(List<GooglePlacesClient.AuthorAttribution> authors) {
        return new RestaurantPhotoAttributionResponse(GOOGLE_MAPS,
                authors.stream().map(author -> new Author(author.displayName(), author.uri())).toList());
    }

    public record Author(String displayName, String uri) {
    }
}
