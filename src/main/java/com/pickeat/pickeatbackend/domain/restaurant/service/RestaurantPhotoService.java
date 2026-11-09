package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.exception.GlobalErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// Google 출처 식당의 대표 사진 주소를 Google Places에서 받아 온다. API 키를 프론트에 노출할 수 없어서
// 백엔드가 주소를 받아 리다이렉트로 넘겨준다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantPhotoService {

    static final int MAX_WIDTH_PX = 800;
    public static final int MAX_PHOTOS = 3;
    static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int MAX_CACHE_ENTRIES = 2000;

    private final RestaurantRepository restaurantRepository;
    private final GooglePlacesClient googlePlacesClient;

    // 사진 한 번 조회마다 Google Place Photo 요금이 붙으므로 받은 주소·촬영자(사진 없음 포함)를 30분간 기억한다.
    // ponytail: 인스턴스 메모리라 재시작하면 비워지고, 가득 차면 통째로 비운다. Google이 준 임시 주소가
    // 30분 안에 만료되면 그동안 이미지가 깨진다. 문제가 되면 TTL을 줄이거나 캐시 라이브러리로 바꾼다.
    private final Map<PhotoKey, CachedPhoto> cache = new ConcurrentHashMap<>();

    public String getPhotoUri(Long restaurantId) {
        return getPhotoUri(restaurantId, 0);
    }

    // photoIndex는 0부터 MAX_PHOTOS - 1까지. 식당 상세에서 여러 장을 보여줄 때 쓴다.
    public String getPhotoUri(Long restaurantId, int photoIndex) {
        return getPhoto(restaurantId, photoIndex).uri();
    }

    // 같은 사진의 촬영자 표시 정보. 사진 주소와 같은 캐시를 쓰므로 사진을 먼저 불렀다면 Google을 다시 부르지 않는다.
    public List<GooglePlacesClient.AuthorAttribution> getPhotoAuthors(Long restaurantId, int photoIndex) {
        return getPhoto(restaurantId, photoIndex).authors();
    }

    private GooglePlacesClient.PlacePhoto getPhoto(Long restaurantId, int photoIndex) {
        if (photoIndex < 0 || photoIndex >= MAX_PHOTOS) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
        }
        PhotoKey key = new PhotoKey(restaurantId, photoIndex);
        CachedPhoto cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.photo().orElseThrow(() -> new BusinessException(RestaurantErrorCode.PHOTO_NOT_FOUND));
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

        Optional<GooglePlacesClient.PlacePhoto> photo;
        try {
            photo = googlePlacesClient.findPhoto(restaurant.getGooglePlaceId(), photoIndex, MAX_WIDTH_PX);
        } catch (RestClientException e) {
            // 일시적인 Google 오류일 수 있으므로 기억하지 않는다.
            log.warn("Google 사진 조회 실패: restaurantId={}, photoIndex={}", restaurantId, photoIndex, e);
            throw new BusinessException(RestaurantErrorCode.PHOTO_NOT_FOUND);
        }

        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(key, new CachedPhoto(photo, Instant.now().plus(CACHE_TTL)));
        return photo.orElseThrow(() -> new BusinessException(RestaurantErrorCode.PHOTO_NOT_FOUND));
    }

    private record PhotoKey(Long restaurantId, int photoIndex) {
    }

    private record CachedPhoto(Optional<GooglePlacesClient.PlacePhoto> photo, Instant expiresAt) {
    }
}
