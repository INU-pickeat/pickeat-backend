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
    Boolean allowsDogs,
    OpeningHours regularOpeningHours
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
            String primaryType,
            List<String> types,
            PriceRange priceRange,
            Boolean goodForChildren,
            Boolean goodForGroups,
            Boolean menuForChildren,
            Boolean allowsDogs
    ) {
        this(id, displayName, formattedAddress, location, rating, userRatingCount, googleMapsUri, attributions,
                primaryType, types, priceRange, goodForChildren, goodForGroups, menuForChildren, allowsDogs, null);
    }

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
                attributions, primaryType, List.of(), null, null, null, null, null, null);
    }

    public record DisplayName(String text, String languageCode) {
    }

    public record Location(Double latitude, Double longitude) {
    }

    public record Attribution(String provider, String providerUri) {
    }

    // day는 0=일요일 ~ 6=토요일. close가 없으면 24시간 영업이다.
    public record OpeningHours(List<Period> periods) {
    }

    public record Period(Point open, Point close) {
    }

    public record Point(Integer day, Integer hour, Integer minute) {
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
