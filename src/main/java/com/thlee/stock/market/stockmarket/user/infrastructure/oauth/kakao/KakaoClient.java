package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoClient {
    private final RestClient restClient;

    public KakaoClient() {
        this.restClient = RestClient.builder().build();
    }

    public RestClient restClient() {
        return restClient;
    }
}
