package com.pickeat.pickeatbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 계정은 SecurityConfig의 관리자·문서 체인이 각자 만든다. 기본 계정 자동 생성은 운영 로그에 비밀번호를 남기므로 끈다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class PickEatBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickEatBackendApplication.class, args);
    }
}
