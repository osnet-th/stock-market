package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 한국자산평가 전용 RestClient. 기본 restClient 빈에는 타임아웃이 없어 별도로 둔다.
 */
@Configuration
public class KoreaApRestClientConfig {

    @Bean("koreaApRestClient")
    public RestClient koreaApRestClient(KoreaApProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .build();
    }
}
