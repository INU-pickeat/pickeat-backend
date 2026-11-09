package com.pickeat.pickeatbackend.domain.restaurant.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantPhotoService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantPhotoApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantPhotoService restaurantPhotoService;

    @Test
    @DisplayName("식당 사진 API는 인증 없이 Google 사진 주소로 302 리다이렉트한다")
    void redirectsToGooglePhotoWithoutAuthentication() throws Exception {
        when(restaurantPhotoService.getPhotoUri(10L, 0)).thenReturn("https://lh3.googleusercontent.com/test-photo");

        mockMvc.perform(get("/api/v1/restaurants/10/photo"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://lh3.googleusercontent.com/test-photo"))
                .andExpect(header().string("Cache-Control", Matchers.containsString("max-age=1800")));
    }

    @Test
    @DisplayName("사진이 없는 식당은 404 RESTAURANT_002")
    void returnsNotFoundWhenRestaurantHasNoPhoto() throws Exception {
        when(restaurantPhotoService.getPhotoUri(10L, 0))
                .thenThrow(new BusinessException(RestaurantErrorCode.PHOTO_NOT_FOUND));

        mockMvc.perform(get("/api/v1/restaurants/10/photo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESTAURANT_002"));
    }

    @Test
    @DisplayName("index 파라미터로 다른 순번의 사진을 요청할 수 있다")
    void redirectsToRequestedPhotoIndex() throws Exception {
        when(restaurantPhotoService.getPhotoUri(10L, 2)).thenReturn("https://lh3.googleusercontent.com/third-photo");

        mockMvc.perform(get("/api/v1/restaurants/10/photo").param("index", "2"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://lh3.googleusercontent.com/third-photo"));
    }

    @Test
    @DisplayName("index가 숫자가 아니면 400 GLOBAL_001")
    void rejectsNonNumericIndex() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/10/photo").param("index", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));
    }

    @Test
    @DisplayName("사진 출처 API는 인증 없이 Google Maps와 촬영자 이름·링크를 돌려준다")
    void returnsPhotoAttributionsWithoutAuthentication() throws Exception {
        when(restaurantPhotoService.getPhotoAuthors(10L, 1)).thenReturn(List.of(
                new GooglePlacesClient.AuthorAttribution("홍길동", "https://maps.google.com/maps/contrib/1")));

        mockMvc.perform(get("/api/v1/restaurants/10/photo/attributions").param("index", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("Google Maps"))
                .andExpect(jsonPath("$.authors[0].displayName").value("홍길동"))
                .andExpect(jsonPath("$.authors[0].uri").value("https://maps.google.com/maps/contrib/1"));
    }

    @Test
    @DisplayName("사진이 없는 식당의 출처 조회는 404 RESTAURANT_002")
    void returnsNotFoundAttributionsWhenRestaurantHasNoPhoto() throws Exception {
        when(restaurantPhotoService.getPhotoAuthors(10L, 0))
                .thenThrow(new BusinessException(RestaurantErrorCode.PHOTO_NOT_FOUND));

        mockMvc.perform(get("/api/v1/restaurants/10/photo/attributions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESTAURANT_002"));
    }
}
