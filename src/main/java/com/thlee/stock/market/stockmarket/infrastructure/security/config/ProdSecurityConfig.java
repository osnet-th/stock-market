package com.thlee.stock.market.stockmarket.infrastructure.security.config;

import com.thlee.stock.market.stockmarket.infrastructure.security.jwt.JwtAuthenticationEntryPoint;
import com.thlee.stock.market.stockmarket.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@Profile("prod")
@RequiredArgsConstructor
public class ProdSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 정책: STATELESS (JWT 기반)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 인증 실패 시 401 응답
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // 인증/인가 설정
            .authorizeHttpRequests(auth -> auth
                // Static 리소스 허용
                .requestMatchers("/", "/index.html", "/login.html", "/signup.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                // 인증 엔드포인트는 permitAll
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/oauth/**").permitAll()

                // Actuator health 엔드포인트는 permitAll
                .requestMatchers("/actuator/health").permitAll()

                // 뉴스 검색 엔드포인트는 permitAll
                .requestMatchers("/api/news/search").permitAll()

                // 나머지 요청은 인증 필요 (백필 포함)
                .anyRequest().authenticated()
            )

            // httpBasic 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)

            // formLogin 비활성화
            .formLogin(AbstractHttpConfigurer::disable)

            // JWT 필터 등록
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 운영 환경: 특정 도메인만 허용 (설정에서 읽어옴)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // 허용 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With"
        ));

        // Credentials 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}