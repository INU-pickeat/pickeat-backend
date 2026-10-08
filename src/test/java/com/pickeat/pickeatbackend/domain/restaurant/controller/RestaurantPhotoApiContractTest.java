package com.pickeat.pickeatbackend.domain.restaurant.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantPhotoService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
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
        when(restaurantPhotoService.getPhotoUri(10L)).thenReturn("https://lh3.googleusercontent.com/test-photo");

        mockMvc.perform(get("/api/v1/restaurants/10/photo"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://lh3.googleusercontent.com/test-photo"))
                .andExpect(header().string("Cache-Control", Matchers.containsString("max-age=1800")));
    }

    @Test
    @DisplayName("사진이 없는 식당은 404 RESTAURANT_002")
    void returnsNotFoundWhenRestaurantHasNoPhoto() throws Exception {
        when(restaurantPhotoService.getPhotoUri(10L))
                .thenThrow(new BusinessException(RestaurantErrorCode.PHOTO_NOT_FOUND));

        mockMvc.perform(get("/api/v1/restaurants/10/photo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESTAURANT_002"));
    }
}
