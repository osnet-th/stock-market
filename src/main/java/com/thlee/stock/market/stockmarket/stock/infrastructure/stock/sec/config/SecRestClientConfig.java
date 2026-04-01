package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * SEC EDGAR API 전용 RestClient 설정.
 * Company Facts 응답이 2-8MB로 크므로 read timeout을 15초로 설정한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecRestClientConfig {

    private final SecProperties secProperties;

    @Bean("secRestClient")
    public RestClient secRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", secProperties.getUserAgent())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}