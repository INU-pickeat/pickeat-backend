package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GooglePlacesClientTest {

    private static final Set<String> RESTAURANT_TYPE = Set.of("restaurant");

    private MockRestServiceServer server;
    private GooglePlacesClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GooglePlacesClient(builder, "test-key");
    }

    @Test
    void sendsFixedRadiusAndMaximumWithExplicitGoogleRanking() {
        server.expect(requestTo("https://places.googleapis.com/v1/places:searchNearby"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Goog-Api-Key", "test-key"))
            .andExpect(header("X-Goog-FieldMask", GooglePlacesClient.FIELD_MASK))
            .andExpect(content().json("""
                {"includedPrimaryTypes":["restaurant"],"maxResultCount":20,"rankPreference":"POPULARITY",
                 "languageCode":"ko","locationRestriction":{"circle":{
                 "center":{"latitude":37.58,"longitude":127.0},"radius":5000.0}}}
                """))
            .andRespond(withSuccess("""
                {"places":[{"id":"test-place","displayName":{"text":"테스트 식당","languageCode":"ko"},
                "location":{"latitude":37.58,"longitude":127.0},"rating":4.5,"userRatingCount":10,
                "googleMapsUri":"https://maps.google.com/test",
                "attributions":[{"provider":"Example","providerUri":"https://example.com"}]}]}
                """, MediaType.APPLICATION_JSON));

        var places = client.findNearbyRestaurants(37.58, 127.0, 5000.0, GooglePlacesClient.RankPreference.POPULARITY, RESTAURANT_TYPE);

        assertThat(places).hasSize(1);
        assertThat(places.getFirst().id()).isEqualTo("test-place");
        assertThat(places.getFirst().displayName().text()).isEqualTo("테스트 식당");
        assertThat(places.getFirst().rating()).isEqualTo(4.5);
        assertThat(places.getFirst().attributions()).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("카테고리별 구체적인 Google 타입을 그대로 전달한다")
    void passesSpecificGoogleTypesPerCategory() {
        Set<String> koreanTypes = Set.of("korean_restaurant", "korean_barbecue_restaurant");
        server.expect(anything())
            .andExpect(jsonPath("$.includedPrimaryTypes", containsInAnyOrder("korean_restaurant", "korean_barbecue_restaurant")))
            .andRespond(withSuccess("{\"places\":[]}", MediaType.APPLICATION_JSON));

        client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, koreanTypes);

        server.verify();
    }

    @Test
    @DisplayName("요청 유형이 비어있으면 Google을 호출하지 않고 실패한다")
    void failsBeforeCallingGoogleWhenIncludedTypesEmpty() {
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, null))
            .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    @DisplayName("Google 제한인 50개를 넘는 주 유형 요청은 호출 전에 거부한다")
    void rejectsMoreThanFiftyPrimaryTypes() {
        Set<String> tooManyTypes = IntStream.rangeClosed(1, 51)
                .mapToObj(index -> "type-" + index)
                .collect(Collectors.toSet());

        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, tooManyTypes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50개");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"places\":[]}"})
    void acceptsSuccessfulEmptyResults(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE)).isEmpty();
        server.verify();
    }

    @Test
    void preservesAllTwentyCandidatesForLaterScoring() {
        String placesJson = IntStream.range(0, 20)
            .mapToObj(index -> "{\"id\":\"place-" + index + "\"}")
            .collect(Collectors.joining(",", "{\"places\":[", "]}"));
        server.expect(anything())
            .andExpect(jsonPath("$.rankPreference").value("DISTANCE"))
            .andRespond(withSuccess(placesJson, MediaType.APPLICATION_JSON));
        var places = client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE);
        assertThat(places).hasSize(20);
        assertThat(places.getLast().id()).isEqualTo("place-19");
        server.verify();
    }

    @Test
    void preservesUnknownRatingInsteadOfInventingZero() {
        server.expect(anything()).andRespond(withSuccess("{\"places\":[{\"id\":\"unrated\"}]}", MediaType.APPLICATION_JSON));
        var places = client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE);
        assertThat(places.getFirst().rating()).isNull();
        assertThat(places.getFirst().userRatingCount()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("가격 범위와 동행 적합 신호를 손실 없이 역직렬화한다")
    void parsesPriceRangeAndCompanionSignals() {
        server.expect(anything()).andRespond(withSuccess("""
                {"places":[{"id":"place-1","priceRange":{
                  "startPrice":{"currencyCode":"KRW","units":"10000","nanos":0},
                  "endPrice":{"currencyCode":"KRW","units":"25000","nanos":0}},
                  "goodForChildren":true,"goodForGroups":true,
                  "menuForChildren":false,"allowsDogs":true}]}
                """, MediaType.APPLICATION_JSON));

        GooglePlaceResponse place = client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE).getFirst();

        assertThat(place.priceRange().startPrice().amount()).isEqualByComparingTo("10000");
        assertThat(place.priceRange().endPrice().amount()).isEqualByComparingTo("25000");
        assertThat(place.goodForChildren()).isTrue();
        assertThat(place.goodForGroups()).isTrue();
        assertThat(place.menuForChildren()).isFalse();
        assertThat(place.allowsDogs()).isTrue();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void propagatesHttpFailureWithoutRetryOrEmptyFallback(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void propagatesNetworkFailure() {
        server.expect(anything()).andRespond(withException(new IOException("timeout")));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void rejectsMissingBody() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NO_CONTENT));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
            .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void rejectsMalformedJson() {
        server.expect(anything()).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void rejectsInvalidInputBeforeCallingGoogle() {
        for (double latitude : new double[] {91, -91, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> client.findNearbyRestaurants(latitude, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 181, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, 5000.0, null, RESTAURANT_TYPE)).isInstanceOf(NullPointerException.class);
        server.verify();
    }

    @Test
    void missingKeyDoesNotPreventConstructionButRejectsSearch() {
        var unconfigured = new GooglePlacesClient(RestClient.builder(), "");
        assertThatThrownBy(() -> unconfigured.findNearbyRestaurants(0, 0, 5000.0, GooglePlacesClient.RankPreference.DISTANCE, RESTAURANT_TYPE))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("GOOGLE_PLACES_API_KEY");
    }

    @Test
    @DisplayName("사진 이름을 Place Details로 새로 받은 뒤 Place Photo로 임시 주소를 받는다")
    void findsPhotoUriThroughDetailsAndMedia() {
        server.expect(requestTo("https://places.googleapis.com/v1/places/test-place"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Goog-Api-Key", "test-key"))
            .andExpect(header("X-Goog-FieldMask", "photos"))
            .andRespond(withSuccess("""
                {"photos":[{"name":"places/test-place/photos/first-ref","widthPx":4000,"heightPx":3000},
                           {"name":"places/test-place/photos/second-ref"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                "https://places.googleapis.com/v1/places/test-place/photos/first-ref/media"
                    + "?maxWidthPx=800&skipHttpRedirect=true"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Goog-Api-Key", "test-key"))
            .andRespond(withSuccess("""
                {"name":"places/test-place/photos/first-ref/media","photoUri":"https://lh3.googleusercontent.com/photo"}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.findPhotoUri("test-place", 800)).contains("https://lh3.googleusercontent.com/photo");
        server.verify();
    }

    @Test
    @DisplayName("사진이 없는 장소는 Place Photo를 부르지 않고 빈 값을 돌려준다")
    void returnsEmptyWhenPlaceHasNoPhotos() {
        server.expect(requestTo("https://places.googleapis.com/v1/places/test-place"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.findPhotoUri("test-place", 800)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("API 키나 장소 ID가 없으면 Google을 부르지 않고 빈 값을 돌려준다")
    void returnsEmptyPhotoWithoutApiKeyOrPlaceId() {
        assertThat(new GooglePlacesClient(RestClient.builder(), "").findPhotoUri("test-place", 800)).isEmpty();
        assertThat(client.findPhotoUri(null, 800)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("사진 순번을 주면 그 순번의 사진 이름으로 Place Photo를 부른다")
    void findsPhotoUriAtRequestedIndex() {
        server.expect(requestTo("https://places.googleapis.com/v1/places/test-place"))
            .andRespond(withSuccess("""
                {"photos":[{"name":"places/test-place/photos/first-ref"},
                           {"name":"places/test-place/photos/second-ref"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                "https://places.googleapis.com/v1/places/test-place/photos/second-ref/media"
                    + "?maxWidthPx=800&skipHttpRedirect=true"))
            .andRespond(withSuccess("""
                {"name":"places/test-place/photos/second-ref/media","photoUri":"https://lh3.googleusercontent.com/second"}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.findPhotoUri("test-place", 1, 800)).contains("https://lh3.googleusercontent.com/second");
        server.verify();
    }

    @Test
    @DisplayName("요청한 순번의 사진이 없으면 Place Photo를 부르지 않고 빈 값을 돌려준다")
    void returnsEmptyWhenPhotoIndexExceedsAvailablePhotos() {
        server.expect(requestTo("https://places.googleapis.com/v1/places/test-place"))
            .andRespond(withSuccess("""
                {"photos":[{"name":"places/test-place/photos/first-ref"}]}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.findPhotoUri("test-place", 2, 800)).isEmpty();
        server.verify();
    }
}
