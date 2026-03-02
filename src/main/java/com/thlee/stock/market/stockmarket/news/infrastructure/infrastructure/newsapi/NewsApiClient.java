package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.newsapi;

import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.NewsApiException;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.newsapi.config.NewsApiProperties;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.newsapi.dto.NewsApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class NewsApiClient {
    private final RestClient restClient;
    private final NewsApiProperties properties;

    public NewsApiResponse search(String keyword, String from) {
        try {
            return restClient.get()
                .uri(properties.getBaseUrl() + "?q={keyword}&apiKey={apiKey}&from={from}&language={language}&sortBy=publishedAt&pageSize={pageSize}",
                    keyword, properties.getApiKey(), from, properties.getLanguage(), properties.getPageSize())
                .retrieve()
                .body(NewsApiResponse.class);
        } catch (RestClientException e) {
            throw new NewsApiException("NewsAPI 호출 실패: " + e.getMessage(), e);
        }
    }
}