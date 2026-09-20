package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.util.List;

public record GooglePlaceResponse(
    String id,
    DisplayName displayName,
    String formattedAddress,
    Location location,
    Double rating,
    Integer userRatingCount,
    String googleMapsUri,
    List<Attribution> attributions,
    String primaryType
) {
    public record DisplayName(String text, String languageCode) {
    }

    public record Location(Double latitude, Double longitude) {
    }

    public record Attribution(String provider, String providerUri) {
    }
}
