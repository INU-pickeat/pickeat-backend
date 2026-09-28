package com.pickeat.pickeatbackend.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "cors.allowed-origins=https://www.pickeat.kr,https://pickeat-*.vercel.app")
@AutoConfigureMockMvc
class CorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("허용된 origin의 preflight 요청은 인증 없이 CORS 헤더를 받는다")
    void allowsPreflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header("Origin", "https://www.pickeat.kr")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://www.pickeat.kr"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("패턴으로 등록한 Vercel 미리보기 origin도 허용한다")
    void allowsPreflightFromVercelPreviewPattern() throws Exception {
        mockMvc.perform(options("/api/v1/recommendations")
                        .header("Origin", "https://pickeat-git-feature-team.vercel.app")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://pickeat-git-feature-team.vercel.app"));
    }

    @Test
    @DisplayName("허용되지 않은 origin의 preflight 요청은 거부한다")
    void rejectsPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
