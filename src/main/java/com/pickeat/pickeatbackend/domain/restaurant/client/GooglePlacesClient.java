package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        + "places.primaryType,places.types,places.priceRange,places.goodForChildren,places.goodForGroups,"
        + "places.menuForChildren,places.allowsDogs,places.regularOpeningHours";

    static final String PHOTO_FIELD_MASK = "photos";

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

    // 카테고리에 매핑된 구체적인 Google 주 유형을 넘겨 Google 서버에서 선처리시킨다.
    public List<GooglePlaceResponse> findNearbyRestaurants(
        double latitude, double longitude, double radiusMeters, RankPreference rankPreference, Set<String> includedPrimaryTypes
    ) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
            || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("유효한 위도와 경도가 필요합니다.");
        }
        Objects.requireNonNull(rankPreference, "Google 후보 정렬 기준이 필요합니다.");
        if (includedPrimaryTypes == null || includedPrimaryTypes.isEmpty()) {
            throw new IllegalArgumentException("Google에 요청할 장소 유형이 최소 1개 필요합니다.");
        }
        if (includedPrimaryTypes.size() > 50) {
            throw new IllegalArgumentException("Google 장소 유형은 요청당 최대 50개까지 허용됩니다.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_PLACES_API_KEY 설정이 필요합니다.");
        }

        Map<String, Object> request = Map.of(
            "includedPrimaryTypes", List.copyOf(includedPrimaryTypes),
            "maxResultCount", 20,
            "rankPreference", rankPreference.name(),
            "languageCode", "ko",
            "locationRestriction", Map.of("circle", Map.of(
                "center", Map.of("latitude", latitude, "longitude", longitude),
                "radius", radiusMeters
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

    // 식당 대표 사진의 임시 주소를 얻는다. Google 약관상 사진 이름(photos[].name)은 저장할 수 없고 만료되므로,
    // 매번 Place Details로 사진 이름을 새로 받은 뒤(photos 필드만 요청) Place Photo로 주소를 받는다.
    // 사진이 없는 장소이거나 API 키가 없으면 빈 값을 돌려준다. 통신 오류는 RestClientException으로 던진다.
    public Optional<String> findPhotoUri(String placeId, int maxWidthPx) {
        if (placeId == null || placeId.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        PhotosResponse details = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/v1/places/" + placeId).build())
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", PHOTO_FIELD_MASK)
            .retrieve()
            .body(PhotosResponse.class);
        if (details == null || details.photos() == null || details.photos().isEmpty()) {
            return Optional.empty();
        }
        String photoName = details.photos().getFirst().name();
        if (photoName == null || !photoName.startsWith("places/")) {
            return Optional.empty();
        }

        PhotoMedia media = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/v1/" + photoName + "/media")
                .queryParam("maxWidthPx", maxWidthPx)
                .queryParam("skipHttpRedirect", true)
                .build())
            .header("X-Goog-Api-Key", apiKey)
            .retrieve()
            .body(PhotoMedia.class);
        return media == null || media.photoUri() == null || media.photoUri().isBlank()
            ? Optional.empty()
            : Optional.of(media.photoUri());
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

    record PhotosResponse(List<Photo> photos) {
        record Photo(String name) {
        }
    }

    record PhotoMedia(String name, String photoUri) {
    }
}
