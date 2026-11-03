package com.pickeat.pickeatbackend.global.security;

import com.pickeat.pickeatbackend.global.security.jwt.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import com.pickeat.pickeatbackend.global.exception.ErrorResponse;
import com.pickeat.pickeatbackend.global.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 관리자 대시보드(/admin.html)와 집계 API는 회원 JWT와 별개로 HTTP Basic으로 보호한다.
     * 계정은 이 체인에서만 쓰는 메모리 사용자 한 명이며 ADMIN_USERNAME/ADMIN_PASSWORD로 바꾼다.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain adminFilterChain(
            HttpSecurity http,
            @Value("${admin.username}") String username,
            @Value("${admin.password}") String password
    ) throws Exception {
        UserDetails admin = User.withUsername(username)
                .password(passwordEncoder().encode(requireAdminPassword(password)))
                .roles("ADMIN")
                .build();
        return http
                .securityMatcher("/admin.html", "/api/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(new InMemoryUserDetailsManager(admin))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/admin.html", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    // ADMIN_PASSWORD가 아예 없으면 설정 해석 단계에서 실패하지만, "ADMIN_PASSWORD="처럼 빈 값은 통과하므로 여기서 막는다.
    static String requireAdminPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD가 비어 있습니다. 관리자 비밀번호를 설정해야 기동합니다.");
        }
        return password;
    }

    @Bean
    @Order(1)
    @Profile("local")
    public SecurityFilterChain localDocumentationFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> writeError(response, GlobalErrorCode.UNAUTHORIZED)
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
                                "/api/v1/auth/email-verifications", "/api/v1/auth/email-verifications/confirm",
                                "/api/v1/auth/password-reset", "/api/v1/auth/password-reset/email-verifications",
                                "/error",
                                "/actuator/health", "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/images/discovery/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/discovery-spots", "/api/v1/discovery-spots/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 필터 단계 오류는 컨트롤러 예외 처리기를 거치지 않으므로 같은 ErrorResponse 형식을 직접 쓴다.
    private void writeError(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {
        ErrorResponse body = ErrorResponse.from(errorCode);
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + body.code() + "\",\"message\":\"" + body.message() + "\"}");
    }

    /**
     * 프론트(Vercel)가 다른 도메인에서 API를 호출할 수 있게 허용한다. 비어 있으면 어떤 origin도 허용하지 않는다.
     * JWT는 Authorization 헤더로만 주고받으므로 쿠키 자격 증명은 허용하지 않는다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:}") List<String> allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
