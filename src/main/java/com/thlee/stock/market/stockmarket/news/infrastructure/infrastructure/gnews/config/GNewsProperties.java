package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "news.api.gnews")
public class GNewsProperties {
    private String baseUrl;
    private String apiKey;
    private String lang = "en";
    private int max = 100;
}