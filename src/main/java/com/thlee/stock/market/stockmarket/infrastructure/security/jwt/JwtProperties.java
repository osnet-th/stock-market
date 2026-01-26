package com.thlee.stock.market.stockmarket.infrastructure.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티
 * application.yml의 jwt 설정을 바인딩
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * JWT 서명 키
     */
    private String secret;

    /**
     * Access Token 만료 시간 (밀리초)
     */
    private long accessTokenExpiration;

    /**
     * Refresh Token 만료 시간 (밀리초)
     */
    private long refreshTokenExpiration;
}