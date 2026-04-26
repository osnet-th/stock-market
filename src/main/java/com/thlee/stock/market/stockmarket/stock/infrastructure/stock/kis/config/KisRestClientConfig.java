package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * KIS API 전용 RestClient 설정.
 * connect/read timeout 을 명시해 응답 지연이 stocknoteSnapshotExecutor 워커 또는 Tomcat 워커로
 * 무한 전파되지 않도록 한다 (DART/SEC 어댑터와 동일 패턴).
 */
@Configuration
@RequiredArgsConstructor
public class KisRestClientConfig {

    private final KisProperties properties;

    @Bean("kisRestClient")
    public RestClient kisRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory).build();
    }
}