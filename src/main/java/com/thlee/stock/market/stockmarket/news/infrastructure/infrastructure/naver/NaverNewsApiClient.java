package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver;

import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.NewsApiException;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver.config.NaverNewsProperties;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver.dto.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
@Component
@RequiredArgsConstructor
public class NaverNewsApiClient {
    private final RestClient restClient;
    private final NaverNewsProperties properties;

    public NaverNewsResponse search(String keyword) {
        try {
            return restClient.get()
                .uri(properties.getBaseUrl() + "?query={keyword}&display={display}&sort=date",
                    keyword, properties.getDisplay())
                .header("X-Naver-Client-Id", properties.getClientId())
                .header("X-Naver-Client-Secret", properties.getClientSecret())
                .retrieve()
                .body(NaverNewsResponse.class);
        } catch (RestClientException e) {
            throw new NewsApiException("Naver 뉴스 API 호출 실패: " + e.getMessage(), e);
        }
    }
}