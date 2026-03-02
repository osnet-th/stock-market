package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.newsapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "news.api.newsapi")
public class NewsApiProperties {
    private String baseUrl;
    private String apiKey;
    private String language = "en";
    private int pageSize = 100;
}