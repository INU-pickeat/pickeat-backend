package com.pickeat.pickeatbackend.domain.discovery.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.discovery.dto.DiscoverySpotsResponse;
import com.pickeat.pickeatbackend.domain.discovery.service.DiscoverySpotService;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DiscoverySpotApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscoverySpotService discoverySpotService;

    @Test
    @DisplayName("탐색 스팟 조회 API는 인증 없이 지역과 식당을 순서대로 반환한다")
    void returnsDiscoverySpotsWithoutAuthentication() throws Exception {
        DiscoverySpotsResponse.RestaurantItem restaurant =
                new DiscoverySpotsResponse.RestaurantItem(
                        1L,
                        "야스노야지로 압구정점",
                        FoodCategory.JAPANESE,
                        "일본 감성 닮은 양고기 오마카세",
                        "서울 강남구 논현로163길 13-5 한가빌딩 1층",
                        "매일 17:00~22:00",
                        "02-515-0818",
                        "/images/discovery/sinsa_01_main.jpg",
                        1
                );
        when(discoverySpotService.getDiscoverySpots()).thenReturn(new DiscoverySpotsResponse(List.of(
                new DiscoverySpotsResponse.Spot("SINSA", "신사", 1, List.of(restaurant))
        )));

        mockMvc.perform(get("/api/v1/discovery-spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spots[0].regionCode").value("SINSA"))
                .andExpect(jsonPath("$.spots[0].regionName").value("신사"))
                .andExpect(jsonPath("$.spots[0].restaurants[0].name").value("야스노야지로 압구정점"))
                .andExpect(jsonPath("$.spots[0].restaurants[0].foodCategory").value("JAPANESE"))
                .andExpect(jsonPath("$.spots[0].restaurants[0].representativeImageUrl")
                        .value("/images/discovery/sinsa_01_main.jpg"));
    }

    @Test
    @DisplayName("탐색 스팟 이미지는 인증 없이 조회할 수 있다")
    void servesDiscoveryImageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/images/discovery/sinsa_01_main.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }
}
