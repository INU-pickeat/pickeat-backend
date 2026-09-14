package com.pickeat.pickeatbackend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // 공식 PostGIS 이미지는 amd64이므로 Apple Silicon에서도 동일한 환경을 사용한다.
        return new PostgreSQLContainer(DockerImageName.parse("postgis/postgis:18-3.6")
            .asCompatibleSubstituteFor("postgres"))
            .withCreateContainerCmdModifier(command -> command.withPlatform("linux/amd64"))
            .withDatabaseName("pick_eat_test");
    }
}
