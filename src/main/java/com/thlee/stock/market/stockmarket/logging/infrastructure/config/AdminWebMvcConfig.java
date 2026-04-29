package com.thlee.stock.market.stockmarket.logging.infrastructure.config;

import com.thlee.stock.market.stockmarket.logging.infrastructure.filter.AdminGuardInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 운영자 전용 인터셉터 등록 + {@link AdminProperties} 활성화.
 *
 * <p>{@code /api/admin/logs/**} (운영자 로그 페이지) 와 {@code /api/admin/dashboard/**}
 * (메인 대시보드 운영자 카드) 두 경로에 {@link AdminGuardInterceptor} 를 연결해 Spring Security
 * 체인 이후의 HandlerInterceptor 단계에서 인가를 수행한다.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final AdminGuardInterceptor adminGuardInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminGuardInterceptor)
                .addPathPatterns("/api/admin/logs/**", "/api/admin/dashboard/**");
    }
}