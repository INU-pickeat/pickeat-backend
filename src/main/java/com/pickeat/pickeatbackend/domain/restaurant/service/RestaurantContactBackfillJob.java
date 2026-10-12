package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.DataProvider;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 전화번호·영업시간 텍스트가 없던 시절에 저장한 Google 식당을 매달 정해진 수만큼 Place Details로 채운다.
// Place Details Enterprise SKU의 월 무료 한도(1,000건) 안에서 끝내려고 기본값을 1,000으로 둔다.
// Google 과금 월은 태평양 시간 기준이라 매월 1일 18시(KST, 태평양 시간 1일 새벽)에 돈다.
// 대상이 없으면 아무것도 호출하지 않으므로 다 채운 뒤에는 꺼 두기만 하면 된다.
@Slf4j
@Component
@ConditionalOnProperty(name = "contact-backfill.enabled", havingValue = "true")
public class RestaurantContactBackfillJob {

    private final RestaurantRepository restaurantRepository;
    private final GooglePlacesClient googlePlacesClient;
    private final int monthlyLimit;

    public RestaurantContactBackfillJob(
            RestaurantRepository restaurantRepository,
            GooglePlacesClient googlePlacesClient,
            @Value("${contact-backfill.monthly-limit:1000}") int monthlyLimit
    ) {
        this.restaurantRepository = restaurantRepository;
        this.googlePlacesClient = googlePlacesClient;
        this.monthlyLimit = monthlyLimit;
    }

    @Scheduled(cron = "${contact-backfill.cron:0 0 18 1 * *}", zone = "Asia/Seoul")
    public void run() {
        List<Restaurant> targets = restaurantRepository.findByDataProviderAndContactFetchedAtIsNullOrderByIdAsc(
                DataProvider.GOOGLE, Limit.of(monthlyLimit));
        int filled = 0;
        int failed = 0;
        for (Restaurant restaurant : targets) {
            try {
                GooglePlaceResponse place = googlePlacesClient.findContact(restaurant.getGooglePlaceId());
                GooglePlaceResponse.OpeningHours hours = place.regularOpeningHours();
                restaurant.updateOpeningHours(OpeningHours.toWeekMinutes(hours));
                restaurant.updateContactFromGoogle(place.nationalPhoneNumber(), OpeningHours.toText(hours));
                restaurantRepository.save(restaurant);
                filled++;
            } catch (RuntimeException exception) {
                // 실패한 식당은 contactFetchedAt이 비어 있으므로 다음 달에 다시 시도한다.
                failed++;
                log.warn("연락처 백필 실패 restaurantId={}", restaurant.getId(), exception);
            }
        }
        log.info("연락처 백필 완료 대상={} 성공={} 실패={}", targets.size(), filled, failed);
    }
}
