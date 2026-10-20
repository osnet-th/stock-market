package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import com.thlee.stock.market.stockmarket.mcp.infrastructure.config.McpOAuthProperties;
import com.thlee.stock.market.stockmarket.mcp.infrastructure.config.McpPaths;
import org.springaicommunity.mcp.security.server.oauth2.authentication.BearerResourceMetadataTokenAuthenticationEntryPoint;
import org.springaicommunity.mcp.security.server.oauth2.metadata.ResourceIdentifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2;

/**
 * /mcp 리소스 서버 보안 체인. 기존 화면·REST 체인보다 먼저 MCP 경로만 받는다(프로필 공통, dev 포함).
 * 토큰이 없거나 틀리면 401 + WWW-Authenticate(resource_metadata)로 응답한다.
 */
@Configuration
@EnableConfigurationProperties(McpOAuthProperties.class)
public class McpResourceServerSecurityConfig {

    public static final int ORDER = 1;

    @Bean
    @Order(ORDER)
    public SecurityFilterChain mcpResourceServerSecurityFilterChain(
            HttpSecurity http, McpOAuthProperties properties, JwtDecoder mcpJwtDecoder) throws Exception {
        http
                .securityMatcher(McpPaths.RESOURCE_SERVER)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(McpPaths.PROTECTED_RESOURCE_METADATA + "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        new BearerResourceMetadataTokenAuthenticationEntryPoint(new ResourceIdentifier(McpPaths.MCP))))
                .with(mcpServerOAuth2(), mcp -> mcp
                        .authorizationServer(properties.getIssuer())
                        .resourcePath(McpPaths.MCP)
                        .resourceName("stock-market")
                        .jwtDecoder(mcpJwtDecoder)
                        .validateAudienceClaim(true));
        return http.build();
    }

    /** 보안 필터보다 먼저 MCP·OAuth 경로의 요청 주소를 공개 주소로 맞춘다 */
    @Bean
    public FilterRegistrationBean<McpPublicOriginFilter> mcpPublicOriginFilter(McpOAuthProperties properties) {
        FilterRegistrationBean<McpPublicOriginFilter> registration =
                new FilterRegistrationBean<>(new McpPublicOriginFilter(properties.getIssuer()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
