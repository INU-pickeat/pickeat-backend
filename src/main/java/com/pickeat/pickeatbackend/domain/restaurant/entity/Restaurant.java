package com.pickeat.pickeatbackend.domain.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SqlResultSetMapping(
        name = "Restaurant.withDistance",
        entities = @EntityResult(entityClass = Restaurant.class),
        columns = @ColumnResult(name = "distance_meters", type = Double.class)
)
@NamedNativeQuery(
        name = "Restaurant.findWithinRadius",
        resultSetMapping = "Restaurant.withDistance",
        query = """
                SELECT r.*, ST_Distance(r.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distance_meters
                FROM restaurants r
                WHERE ST_DWithin(r.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
                ORDER BY distance_meters ASC
                """
)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataProvider dataProvider;

    @Column(unique = true, length = 255)
    private String googlePlaceId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FoodCategory foodCategory;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 50)
    private String phoneNumber;

    @Column(columnDefinition = "text")
    private String openingHoursText;

    @Column(precision = 2, scale = 1)
    private BigDecimal externalRating;

    private Integer externalRatingCount;

    @Column(length = 30)
    private String priceLevel;

    @Column(precision = 19, scale = 2)
    private BigDecimal priceRangeStart;

    @Column(precision = 19, scale = 2)
    private BigDecimal priceRangeEnd;

    @Column(length = 3)
    private String priceCurrencyCode;

    @Column(length = 1000)
    private String representativeImageUrl;

    private Instant externalDataRefreshedAt;

    private Boolean suitableForDate;
    private Boolean suitableForFamily;
    private Boolean suitableForChildren;
    private Boolean suitableForSolo;
    private Boolean suitableForGroup;
    private Boolean suitableForDogs;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    public Restaurant(
            DataProvider dataProvider,
            String googlePlaceId,
            String name,
            FoodCategory foodCategory,
            String address,
            Double latitude,
            Double longitude,
            BigDecimal externalRating,
            Integer externalRatingCount,
            BigDecimal priceRangeStart,
            BigDecimal priceRangeEnd,
            String priceCurrencyCode,
            Boolean suitableForDate,
            Boolean suitableForFamily,
            Boolean suitableForChildren,
            Boolean suitableForSolo,
            Boolean suitableForGroup,
            Boolean suitableForDogs
    ) {
        this.dataProvider = dataProvider == null ? DataProvider.GOOGLE : dataProvider;
        this.googlePlaceId = googlePlaceId;
        this.name = Objects.requireNonNull(name, "식당명은 필수입니다.");
        this.foodCategory = foodCategory;
        this.address = address;
        this.latitude = Objects.requireNonNull(latitude, "위도는 필수입니다.");
        this.longitude = Objects.requireNonNull(longitude, "경도는 필수입니다.");
        this.externalRating = externalRating;
        this.externalRatingCount = externalRatingCount;
        this.priceRangeStart = priceRangeStart;
        this.priceRangeEnd = priceRangeEnd;
        this.priceCurrencyCode = priceCurrencyCode;
        this.suitableForDate = suitableForDate;
        this.suitableForFamily = suitableForFamily;
        this.suitableForChildren = suitableForChildren;
        this.suitableForSolo = suitableForSolo;
        this.suitableForGroup = suitableForGroup;
        this.suitableForDogs = suitableForDogs;
    }

    // 큐레이션 적합도 필드(suitableFor*)는 내부 편집 값이라 Google 갱신으로 덮어쓰지 않는다.
    public void updateFromGoogle(
            String name,
            FoodCategory foodCategory,
            String address,
            Double latitude,
            Double longitude,
            BigDecimal externalRating,
            Integer externalRatingCount,
            BigDecimal priceRangeStart,
            BigDecimal priceRangeEnd,
            String priceCurrencyCode,
            Boolean googleSuitableForFamily,
            Boolean googleSuitableForChildren,
            Boolean googleSuitableForGroup,
            Boolean googleSuitableForDogs
    ) {
        this.name = Objects.requireNonNull(name, "식당명은 필수입니다.");
        this.foodCategory = foodCategory;
        this.address = address;
        this.latitude = Objects.requireNonNull(latitude, "위도는 필수입니다.");
        this.longitude = Objects.requireNonNull(longitude, "경도는 필수입니다.");
        this.externalRating = externalRating;
        this.externalRatingCount = externalRatingCount;
        this.priceRangeStart = priceRangeStart;
        this.priceRangeEnd = priceRangeEnd;
        this.priceCurrencyCode = priceCurrencyCode;
        if (this.suitableForFamily == null) {
            this.suitableForFamily = googleSuitableForFamily;
        }
        if (this.suitableForChildren == null) {
            this.suitableForChildren = googleSuitableForChildren;
        }
        if (this.suitableForGroup == null) {
            this.suitableForGroup = googleSuitableForGroup;
        }
        if (this.suitableForDogs == null) {
            this.suitableForDogs = googleSuitableForDogs;
        }
        this.externalDataRefreshedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
