package com.thlee.stock.market.stockmarket.realestate.infrastructure.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * realestate 도메인 전용 RestClient.
 * <p>
 * Apache HttpClient 5 기반 — JDK HttpURLConnection은 socket read 중 interrupt를 무시하므로
 * future.cancel(true)이 좀비 워커를 정리하지 못하는 문제(P0 #3)를 회피한다.
 * connect 5s / response 30s 명시 — 외부 공공 API hang 시 30초 후 socket close 보장.
 */
@Configuration
public class RealEstateRestClientConfig {

    public static final String CLIENT_BEAN = "realEstateRestClient";

    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;
    private static final int MAX_TOTAL = 50;
    private static final int MAX_PER_ROUTE = 10;

    @Bean(name = CLIENT_BEAN)
    public RestClient realEstateRestClient() {
        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(MAX_TOTAL)
                .setMaxConnPerRoute(MAX_PER_ROUTE)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}