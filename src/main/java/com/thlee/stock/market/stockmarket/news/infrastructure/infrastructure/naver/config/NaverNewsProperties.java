package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "news.api.naver")
public class NaverNewsProperties {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private int display = 100;
}