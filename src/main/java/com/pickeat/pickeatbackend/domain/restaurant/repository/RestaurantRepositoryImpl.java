package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RestaurantRepositoryImpl implements RestaurantRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    // ST_DWithin(geography, geography, meters)로 GiST 인덱스(restaurants_location_gist_idx)를 태울 수 있는
    // 형태를 유지한다. 반경은 넘겨받은 값 그대로만 쓰고 자동으로 넓히지 않는다.
    @Override
    @SuppressWarnings("unchecked")
    public List<RestaurantCandidate> findWithinRadius(double latitude, double longitude, double radiusMeters) {
        List<Object[]> rows = entityManager.createNamedQuery("Restaurant.findWithinRadius")
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("radiusMeters", radiusMeters)
                .getResultList();

        return rows.stream()
                .map(row -> new RestaurantCandidate((Restaurant) row[0], (Double) row[1]))
                .toList();
    }
}
