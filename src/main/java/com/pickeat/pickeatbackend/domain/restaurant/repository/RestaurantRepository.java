package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.entity.DataProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantRepositoryCustom {

    Optional<Restaurant> findByGooglePlaceId(String googlePlaceId);

    List<Restaurant> findByDataProviderAndContactFetchedAtIsNullOrderByIdAsc(DataProvider dataProvider, Limit limit);

    // 탐색 스팟 식당이면 운영자 한줄평(discovery_spot_restaurants.one_line_intro), 아니면 빈 값.
    @Query(value = """
            SELECT dsr.one_line_intro FROM discovery_spot_restaurants dsr
            WHERE dsr.restaurant_id = :restaurantId
            ORDER BY dsr.id
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findCuratedOneLineIntro(@Param("restaurantId") Long restaurantId);
}
