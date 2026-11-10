package com.pickeat.pickeatbackend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "docs.password=docs123"
})
@AutoConfigureMockMvc
class DocumentationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("운영 Swagger 문서는 문서 계정 비밀번호가 맞을 때만 열린다")
    void requiresDocsAccountForApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs").with(httpBasic("admin", "wrong"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs").with(httpBasic("admin", "docs123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }

    @Test
    @DisplayName("Spring 기본 계정을 만들지 않는다(운영 로그에 생성 비밀번호가 남지 않게)")
    void doesNotCreateDefaultUser() {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
