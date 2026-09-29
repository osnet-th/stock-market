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
 * JDK HttpClient 기본값(HTTP/2)으로는 이 사이트 요청이 응답 없이 타임아웃되고 HTTP/1.1은 정상 응답해 HTTP/1.1로 고정한다.
 */
@Configuration
public class KoreaApRestClientConfig {

    @Bean("koreaApRestClient")
    public RestClient koreaApRestClient(KoreaApProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
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
