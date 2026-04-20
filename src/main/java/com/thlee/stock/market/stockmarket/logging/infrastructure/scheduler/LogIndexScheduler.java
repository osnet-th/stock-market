package com.thlee.stock.market.stockmarket.logging.infrastructure.scheduler;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.LogMonthlyIndexNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 월 단위 로그 인덱스 보관/생성 스케줄러.
 *
 * <ul>
 *   <li><b>cleanupExpiredMonthlyIndices</b> — 매일 03:00 (KST). 현재 UTC 연월 기준 직전월까지는 유지하고
 *       그 이전의 {@code app-audit/error/business-YYYY.MM} 인덱스를 exact name 으로 DELETE.
 *       wildcard delete 는 금지 (cluster 설정 {@code action.destructive_requires_name=true} 와 일관).</li>
 *   <li><b>precreateNextMonthIndices</b> — 매일 23:55 (KST). 다음달 인덱스가 없으면 생성.
 *       매일 수행해도 이미 존재 시 noop 이므로 비용 낮음 ( Spring cron 으로 "매월 말일" 표현 어려움 대응).</li>
 * </ul>
 *
 * 모든 ES 호출은 best-effort — 실패 시 WARN 만 남기고 다음 iteration 으로 넘어간다.
 * (로그 인덱스 정리 실패가 본 서비스를 막아서는 안 됨)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogIndexScheduler {

    private static final String INDEX_PREFIX = "app-";
    private static final int PARTS_EXPECTED = 3;   // app | domain | YYYY.MM

    private final ElasticsearchClient elasticsearchClient;
    private final LogMonthlyIndexNameResolver indexNameResolver;

    @Scheduled(cron = "${scheduler.logging.cleanup.cron:0 0 3 * * *}")
    public void cleanupExpiredMonthlyIndices() {
        try (var ctx = LoggingContext.forScheduler("log-index-cleanup")) {
            doCleanup();
        }
    }

    private void doCleanup() {
        YearMonth keepFrom = YearMonth.now(ZoneOffset.UTC).minusMonths(1);  // 직전월까지 유지
        List<String> patterns = List.of(
                INDEX_PREFIX + LogDomain.AUDIT.getIndexSuffix() + "-*",
                INDEX_PREFIX + LogDomain.ERROR.getIndexSuffix() + "-*",
                INDEX_PREFIX + LogDomain.BUSINESS.getIndexSuffix() + "-*"
        );
        try {
            IndicesResponse resp = elasticsearchClient.cat().indices(c -> c.index(patterns));
            for (IndicesRecord record : resp.indices()) {
                String indexName = record.index();
                if (indexName == null) {
                    continue;
                }
                YearMonth indexMonth = parseYearMonth(indexName);
                if (indexMonth == null) {
                    continue;
                }
                if (indexMonth.isBefore(keepFrom)) {
                    deleteIndexSafely(indexName);
                }
            }
        } catch (Exception e) {
            log.warn("로그 인덱스 cleanup 실패 (계속 진행): {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${scheduler.logging.precreate.cron:0 55 23 * * *}")
    public void precreateNextMonthIndices() {
        try (var ctx = LoggingContext.forScheduler("log-index-precreate")) {
            doPrecreate();
        }
    }

    private void doPrecreate() {
        YearMonth next = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        Instant nextMonthInstant = next.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        for (LogDomain domain : LogDomain.values()) {
            String indexName = indexNameResolver.resolve(domain, nextMonthInstant);
            try {
                boolean exists = elasticsearchClient.indices()
                        .exists(e -> e.index(indexName))
                        .value();
                if (!exists) {
                    elasticsearchClient.indices().create(c -> c.index(indexName));
                    log.info("다음달 로그 인덱스 pre-create: {}", indexName);
                }
            } catch (Exception e) {
                log.warn("로그 인덱스 pre-create 실패 (계속 진행): index={}, err={}", indexName, e.getMessage());
            }
        }
    }

    private YearMonth parseYearMonth(String indexName) {
        String[] parts = indexName.split("-");
        if (parts.length != PARTS_EXPECTED) {
            return null;
        }
        String yearMonth = parts[2];     // "2026.04"
        String[] ym = yearMonth.split("\\.");
        if (ym.length != 2) {
            return null;
        }
        try {
            return YearMonth.of(Integer.parseInt(ym[0]), Integer.parseInt(ym[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void deleteIndexSafely(String indexName) {
        try {
            elasticsearchClient.indices().delete(d -> d.index(indexName));
            log.info("만료 로그 인덱스 DELETE: {}", indexName);
        } catch (Exception e) {
            log.warn("로그 인덱스 DELETE 실패 (계속 진행): index={}, err={}", indexName, e.getMessage());
        }
    }
}