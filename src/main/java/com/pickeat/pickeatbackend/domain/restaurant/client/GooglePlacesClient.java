package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GooglePlacesClient {

    static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,"
        + "places.location,places.rating,places.userRatingCount,places.googleMapsUri,places.attributions,"
        + "places.primaryType";

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public GooglePlacesClient(@Value("${google.places.api-key:}") String apiKey) {
        this(createBuilder(), apiKey);
    }

    GooglePlacesClient(RestClient.Builder builder, String apiKey) {
        this.restClient = builder.baseUrl("https://places.googleapis.com").build();
        this.apiKey = apiKey;
    }

    public List<GooglePlaceResponse> findNearbyRestaurants(
        double latitude, double longitude, RankPreference rankPreference
    ) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
            || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("유효한 위도와 경도가 필요합니다.");
        }
        Objects.requireNonNull(rankPreference, "Google 후보 정렬 기준이 필요합니다.");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_PLACES_API_KEY 설정이 필요합니다.");
        }

        // Google의 restaurant 하위 타입을 폭넓게 받아온 뒤 FoodCategory.fromGooglePrimaryType()으로 분류한다.
        Map<String, Object> request = Map.of(
            "includedTypes", List.of("restaurant"),
            "maxResultCount", 20,
            "rankPreference", rankPreference.name(),
            "languageCode", "ko",
            "locationRestriction", Map.of("circle", Map.of(
                "center", Map.of("latitude", latitude, "longitude", longitude),
                "radius", 5000.0
            ))
        );

        NearbyResponse response = restClient.post()
            .uri("/v1/places:searchNearby")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", FIELD_MASK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(NearbyResponse.class);

        // 정상 빈 JSON과 통신/응답 오류를 구분한다. 재시도나 DB 대체 조회는 하지 않는다.
        if (response == null) {
            throw new IllegalStateException("Google Places 응답 본문이 없습니다.");
        }
        return response.places() == null ? List.of() : List.copyOf(response.places());
    }

    private static RestClient.Builder createBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(requestFactory);
    }

    public enum RankPreference {
        POPULARITY, DISTANCE
    }

    record NearbyResponse(List<GooglePlaceResponse> places) {
    }
}
