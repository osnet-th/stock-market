package com.thlee.stock.market.stockmarket.logging.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.LogElasticsearchSearcher;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogDailyCountResponse;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogDiskUsageResponse;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogSearchRequest;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 로그 검색/집계/디스크 조회 서비스.
 *
 * <ul>
 *   <li>검색 window 강제: {@code to - from ≤ 90일}</li>
 *   <li>기본 window: 최근 24시간 (from/to null 시)</li>
 *   <li>size clamp: 1..100 (기본 20)</li>
 *   <li>집계/디스크 결과는 60초 Caffeine 캐시 (대시보드 연타 시 ES 부하 차단)</li>
 *   <li>ES 장애는 빈 응답으로 graceful degrade (운영자 페이지 블랭크 방지)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSearchService {

    private static final Duration MAX_WINDOW = Duration.ofDays(90);
    private static final Duration DEFAULT_WINDOW = Duration.ofHours(24);
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 20;

    private final LogElasticsearchSearcher searcher;

    private final Cache<String, LogDailyCountResponse> aggCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(200)
            .build();

    private final Cache<String, LogDiskUsageResponse> diskCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(1)
            .build();

    public LogSearchResponse search(LogDomain domain, Instant from, Instant to, Long userId,
                                    String q, Integer status, String exceptionClass,
                                    int size, List<Object> searchAfter) {
        Instant[] window = resolveWindow(from, to);
        int resolvedSize = clampSize(size);
        LogSearchRequest req = new LogSearchRequest(
                domain, window[0], window[1], userId, q, status, exceptionClass, resolvedSize, searchAfter
        );
        try {
            return searcher.search(req);
        } catch (Exception e) {
            log.warn("ES 로그 검색 실패: domain={}, err={}", domain, e.getMessage());
            return new LogSearchResponse(Collections.emptyList(), 0L, null);
        }
    }

    public LogDailyCountResponse aggregateByDate(LogDomain domain, Instant from, Instant to) {
        Instant[] window = resolveWindow(from, to);
        String key = domain.name() + "|" + window[0].toEpochMilli() + "|" + window[1].toEpochMilli();
        return aggCache.get(key, k -> fetchAggregate(domain, window[0], window[1]));
    }

    public LogDiskUsageResponse diskUsage() {
        return diskCache.get("usage", k -> fetchDiskUsage());
    }

    // ──────────────────────────────────────────────────────────────────

    private LogDailyCountResponse fetchAggregate(LogDomain domain, Instant from, Instant to) {
        try {
            return searcher.aggregateByDate(domain, from, to);
        } catch (Exception e) {
            log.warn("ES 로그 집계 실패: domain={}, err={}", domain, e.getMessage());
            return new LogDailyCountResponse("day", Collections.emptyList());
        }
    }

    private LogDiskUsageResponse fetchDiskUsage() {
        try {
            return searcher.diskUsage();
        } catch (Exception e) {
            log.warn("ES 디스크 사용량 조회 실패: {}", e.getMessage());
            return new LogDiskUsageResponse(0L, 0L, List.of());
        }
    }

    private Instant[] resolveWindow(Instant from, Instant to) {
        Instant resolvedTo = to != null ? to : Instant.now();
        Instant resolvedFrom = from != null ? from : resolvedTo.minus(DEFAULT_WINDOW);
        if (resolvedFrom.isAfter(resolvedTo)) {
            resolvedFrom = resolvedTo.minus(DEFAULT_WINDOW);
        }
        if (Duration.between(resolvedFrom, resolvedTo).compareTo(MAX_WINDOW) > 0) {
            resolvedFrom = resolvedTo.minus(MAX_WINDOW);
        }
        return new Instant[]{resolvedFrom, resolvedTo};
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}