package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String tokenUri,
    String userInfoUri
) {
}