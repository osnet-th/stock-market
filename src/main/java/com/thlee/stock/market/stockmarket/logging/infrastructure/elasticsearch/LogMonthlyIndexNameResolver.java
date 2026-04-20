package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 월 단위 로그 인덱스명 생성기.
 *
 * 인덱스 라우팅은 문서 {@code timestamp} 의 UTC 기준 연월에 따른다.
 * KST 기준으로 라우팅하면 저장값(UTC)과 인덱스 경계가 불일치하므로 UTC 단일 기준 유지.
 *
 * 형식: {@code app-{domain}-YYYY.MM} (예: {@code app-audit-2026.04})
 */
@Component
public class LogMonthlyIndexNameResolver {

    private static final String INDEX_PREFIX = "app-";
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy.MM");

    /**
     * 로그의 timestamp 기반 인덱스명 반환.
     */
    public String resolve(ApplicationLog log) {
        return resolve(log.domain(), log.timestamp());
    }

    /**
     * 도메인 + 특정 시각 기반 인덱스명 반환.
     */
    public String resolve(LogDomain domain, Instant timestamp) {
        String yearMonth = timestamp.atOffset(ZoneOffset.UTC).format(YEAR_MONTH);
        return INDEX_PREFIX + domain.getIndexSuffix() + "-" + yearMonth;
    }
}