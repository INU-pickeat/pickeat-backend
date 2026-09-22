package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.math.BigDecimal;
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
    String primaryType,
    List<String> types,
    PriceRange priceRange,
    Boolean goodForChildren,
    Boolean goodForGroups,
    Boolean menuForChildren,
    Boolean allowsDogs
) {
    public GooglePlaceResponse(
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
        this(id, displayName, formattedAddress, location, rating, userRatingCount, googleMapsUri,
                attributions, primaryType, List.of(), null, null, null, null, null);
    }

    public record DisplayName(String text, String languageCode) {
    }

    public record Location(Double latitude, Double longitude) {
    }

    public record Attribution(String provider, String providerUri) {
    }

    public record PriceRange(Money startPrice, Money endPrice) {
    }

    public record Money(String currencyCode, String units, Integer nanos) {
        public BigDecimal amount() {
            BigDecimal whole = units == null ? BigDecimal.ZERO : new BigDecimal(units);
            BigDecimal fraction = nanos == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(nanos, 9);
            return whole.add(fraction);
        }
    }
}
