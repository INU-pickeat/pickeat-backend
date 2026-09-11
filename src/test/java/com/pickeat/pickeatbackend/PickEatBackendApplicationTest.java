package com.pickeat.pickeatbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDatabaseConfig.class)
class PickEatBackendApplicationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void connectsToIsolatedDatabaseAndAppliesMigration() {
        assertThat(jdbcTemplate.queryForObject("SELECT current_database()", String.class))
            .isEqualTo("pick_eat_test");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class))
            .isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM restaurants", Long.class))
            .isZero();
    }

    @Test
    void documentationIsDisabledByDefaultEvenForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/v3/api-docs").with(user("tester")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html").with(user("tester")))
            .andExpect(status().isNotFound());
    }

    @Test
    void applicationRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
            .andExpect(status().isUnauthorized());
    }
}
