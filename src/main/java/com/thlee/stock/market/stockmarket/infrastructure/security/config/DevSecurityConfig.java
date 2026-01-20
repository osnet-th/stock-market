package com.thlee.stock.market.stockmarket.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (개발 환경)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 정책: STATELESS (JWT 대비)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 모든 요청 허용 (개발 환경)
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll()
            )

            // httpBasic 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)

            // formLogin 비활성화
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // 개발 환경: 모든 origin 허용
        configuration.addAllowedOriginPattern("*");

        // 허용 메서드
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("PATCH");
        configuration.addAllowedMethod("OPTIONS");

        // 허용 헤더
        configuration.addAllowedHeader("Authorization");
        configuration.addAllowedHeader("Content-Type");
        configuration.addAllowedHeader("*");

        // Credentials 허용
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
            new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}