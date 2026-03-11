package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;

/**
 * DART API 전용 RestClient 설정.
 * DART 서버(opendart.fss.or.kr)는 TLS 1.2 + AES128-GCM-SHA256을 사용하며,
 * 기본 Reactor Netty 클라이언트와 SSL 핸드셰이크가 실패하므로
 * JDK HttpClient 기반으로 별도 구성한다.
 */
@Configuration
public class DartRestClientConfig {

    @Bean("dartRestClient")
    public RestClient dartRestClient() throws NoSuchAlgorithmException {
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(SSLContext.getDefault())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}