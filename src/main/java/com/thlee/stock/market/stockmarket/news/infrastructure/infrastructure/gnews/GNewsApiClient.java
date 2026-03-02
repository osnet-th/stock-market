package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews;

import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.NewsApiException;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.config.GNewsProperties;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.dto.GNewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class GNewsApiClient {
    private final RestClient restClient;
    private final GNewsProperties properties;

    public GNewsResponse search(String keyword, String from) {
        try {
            return restClient.get()
                .uri(properties.getBaseUrl() + "?q={keyword}&apikey={apikey}&lang={lang}&in=title&from={from}&max={max}&sortby=publishedAt",
                    keyword, properties.getApiKey(), properties.getLang(), from, properties.getMax())
                .retrieve()
                .body(GNewsResponse.class);
        } catch (RestClientException e) {
            throw new NewsApiException("GNews API 호출 실패: " + e.getMessage(), e);
        }
    }
}