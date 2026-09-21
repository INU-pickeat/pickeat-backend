package com.pickeat.pickeatbackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PickEatBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("인증 API는 토큰 없이 요청할 수 있다")
    void allowsAuthApiWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일반 API는 인증이 필요하다")
    void requiresAuthenticationForGeneralApi() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("식당 지도 링크 조회는 토큰 없이 요청할 수 있다")
    void allowsRestaurantNavigationLinksWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/999999/navigation-links"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("기본 프로필에서는 API 문서를 노출하지 않는다")
    void hidesApiDocsOnDefaultProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

}
