package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

/**
 * Elasticsearch 클라이언트 설정
 */
@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch"
)
public class NewsElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUri;

    @Value("${spring.elasticsearch.connection-timeout:3s}")
    private Duration connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:5s}")
    private Duration socketTimeout;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(esUri.replace("http://", "").replace("https://", ""))
                .withConnectTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout)
                .build();
    }
}