package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "kakao.oauth")
@Getter
@Setter
public class KakaoOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUri;
    private String issuer;
    private String audience;
}
