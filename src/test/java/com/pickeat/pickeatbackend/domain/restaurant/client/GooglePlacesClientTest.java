package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GooglePlacesClientTest {

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
                {"includedTypes":["restaurant"],"maxResultCount":20,"rankPreference":"POPULARITY",
                 "languageCode":"ko","locationRestriction":{"circle":{
                 "center":{"latitude":37.58,"longitude":127.0},"radius":5000.0}}}
                """))
            .andRespond(withSuccess("""
                {"places":[{"id":"test-place","displayName":{"text":"테스트 식당","languageCode":"ko"},
                "location":{"latitude":37.58,"longitude":127.0},"rating":4.5,"userRatingCount":10,
                "googleMapsUri":"https://maps.google.com/test",
                "attributions":[{"provider":"Example","providerUri":"https://example.com"}]}]}
                """, MediaType.APPLICATION_JSON));

        var places = client.findNearbyRestaurants(37.58, 127.0, GooglePlacesClient.RankPreference.POPULARITY);

        assertThat(places).hasSize(1);
        assertThat(places.getFirst().id()).isEqualTo("test-place");
        assertThat(places.getFirst().displayName().text()).isEqualTo("테스트 식당");
        assertThat(places.getFirst().rating()).isEqualTo(4.5);
        assertThat(places.getFirst().attributions()).hasSize(1);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"places\":[]}"})
    void acceptsSuccessfulEmptyResults(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE)).isEmpty();
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
        var places = client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE);
        assertThat(places).hasSize(20);
        assertThat(places.getLast().id()).isEqualTo("place-19");
        server.verify();
    }

    @Test
    void preservesUnknownRatingInsteadOfInventingZero() {
        server.expect(anything()).andRespond(withSuccess("{\"places\":[{\"id\":\"unrated\"}]}", MediaType.APPLICATION_JSON));
        var places = client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE);
        assertThat(places.getFirst().rating()).isNull();
        assertThat(places.getFirst().userRatingCount()).isNull();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void propagatesHttpFailureWithoutRetryOrEmptyFallback(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void propagatesNetworkFailure() {
        server.expect(anything()).andRespond(withException(new IOException("timeout")));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void rejectsMissingBody() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NO_CONTENT));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE))
            .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void rejectsMalformedJson() {
        server.expect(anything()).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE))
            .isInstanceOf(RestClientException.class);
        server.verify();
    }

    @Test
    void rejectsInvalidInputBeforeCallingGoogle() {
        for (double latitude : new double[] {91, -91, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> client.findNearbyRestaurants(latitude, 0, GooglePlacesClient.RankPreference.DISTANCE))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 181, GooglePlacesClient.RankPreference.DISTANCE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.findNearbyRestaurants(0, 0, null)).isInstanceOf(NullPointerException.class);
        server.verify();
    }

    @Test
    void missingKeyDoesNotPreventConstructionButRejectsSearch() {
        var unconfigured = new GooglePlacesClient(RestClient.builder(), "");
        assertThatThrownBy(() -> unconfigured.findNearbyRestaurants(0, 0, GooglePlacesClient.RankPreference.DISTANCE))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("GOOGLE_PLACES_API_KEY");
    }
}
