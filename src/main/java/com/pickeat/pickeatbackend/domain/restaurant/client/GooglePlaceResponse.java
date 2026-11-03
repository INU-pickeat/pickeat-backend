package com.pickeat.pickeatbackend.domain.restaurant.client;

import java.math.BigDecimal;
import java.util.Arrays;
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
    OpeningHours regularOpeningHours,
    String nationalPhoneNumber,
    ParkingOptions parkingOptions
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
            Boolean allowsDogs,
            OpeningHours regularOpeningHours,
            String nationalPhoneNumber
    ) {
        this(id, displayName, formattedAddress, location, rating, userRatingCount, googleMapsUri, attributions,
                primaryType, types, priceRange, goodForChildren, goodForGroups, menuForChildren, allowsDogs,
                regularOpeningHours, nationalPhoneNumber, null);
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
            String primaryType,
            List<String> types,
            PriceRange priceRange,
            Boolean goodForChildren,
            Boolean goodForGroups,
            Boolean menuForChildren,
            Boolean allowsDogs,
            OpeningHours regularOpeningHours
    ) {
        this(id, displayName, formattedAddress, location, rating, userRatingCount, googleMapsUri, attributions,
                primaryType, types, priceRange, goodForChildren, goodForGroups, menuForChildren, allowsDogs,
                regularOpeningHours, null, null);
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
            String primaryType,
            List<String> types,
            PriceRange priceRange,
            Boolean goodForChildren,
            Boolean goodForGroups,
            Boolean menuForChildren,
            Boolean allowsDogs
    ) {
        this(id, displayName, formattedAddress, location, rating, userRatingCount, googleMapsUri, attributions,
                primaryType, types, priceRange, goodForChildren, goodForGroups, menuForChildren, allowsDogs,
                null, null, null);
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
                attributions, primaryType, List.of(), null, null, null, null, null, null, null, null);
    }

    public record DisplayName(String text, String languageCode) {
    }

    public record Location(Double latitude, Double longitude) {
    }

    public record Attribution(String provider, String providerUri) {
    }

    // day는 0=일요일 ~ 6=토요일. close가 없으면 24시간 영업이다.
    // weekdayDescriptions는 languageCode=ko 기준 "월요일: 오전 11:00~오후 9:00" 형식의 요일별 7줄이다.
    public record OpeningHours(List<Period> periods, List<String> weekdayDescriptions) {
        public OpeningHours(List<Period> periods) {
            this(periods, null);
        }
    }

    public record Period(Point open, Point close) {
    }

    public record Point(Integer day, Integer hour, Integer minute) {
    }

    // 값마다 null일 수 있다(Google이 모르는 항목은 내려주지 않는다).
    public record ParkingOptions(
            Boolean freeParkingLot,
            Boolean paidParkingLot,
            Boolean freeStreetParking,
            Boolean paidStreetParking,
            Boolean valetParking,
            Boolean freeGarageParking,
            Boolean paidGarageParking
    ) {
        // 하나라도 true면 주차 가능, 받은 값이 모두 false면 불가, 받은 값이 없으면 모름(null).
        public Boolean available() {
            List<Boolean> values = Arrays.asList(freeParkingLot, paidParkingLot, freeStreetParking,
                    paidStreetParking, valetParking, freeGarageParking, paidGarageParking);
            if (values.contains(Boolean.TRUE)) {
                return true;
            }
            return values.contains(Boolean.FALSE) ? false : null;
        }
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
