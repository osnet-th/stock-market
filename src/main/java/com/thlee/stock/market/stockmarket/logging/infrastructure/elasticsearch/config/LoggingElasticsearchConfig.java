package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * 로깅 모듈의 Elasticsearch Repository 스캔 설정.
 *
 * ES 클라이언트 빈 자체는 {@code NewsElasticsearchConfig} 가 이미 제공한다.
 * 본 설정은 로깅 패키지 내 Repository 인터페이스 스캔만 활성화하며
 * 향후 Repository 도입 시 대비용으로 유지한다.
 *
 * 현재 단계({@link com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.LogElasticsearchIndexer})
 * 는 {@code ElasticsearchOperations} 를 직접 사용하므로 스캔 대상 Repository 는 없다.
 */
@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch"
)
public class LoggingElasticsearchConfig {
}