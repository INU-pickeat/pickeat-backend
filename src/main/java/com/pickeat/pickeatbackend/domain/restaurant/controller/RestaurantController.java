package com.pickeat.pickeatbackend.domain.restaurant.controller;

import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantNavigationLinksResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantResponse;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantPhotoService;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantService;
import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantPhotoService restaurantPhotoService;

    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
    }

    // <img src>로 바로 쓸 수 있게 Google 사진 주소로 리다이렉트한다. 브라우저도 30분간 다시 묻지 않는다.
    @GetMapping("/{restaurantId}/photo")
    public ResponseEntity<Void> getPhoto(@PathVariable Long restaurantId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(restaurantPhotoService.getPhotoUri(restaurantId)))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)).cachePrivate())
                .build();
    }

    @GetMapping("/{restaurantId}/navigation-links")
    public ResponseEntity<RestaurantNavigationLinksResponse> getNavigationLinks(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getNavigationLinks(restaurantId));
    }
}
