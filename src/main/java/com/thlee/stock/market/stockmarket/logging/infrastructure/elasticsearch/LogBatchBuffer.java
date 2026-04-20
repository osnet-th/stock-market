package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.document.ApplicationLogDocument;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.mapper.LogDocumentMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 로그 적재 배치 버퍼.
 *
 * 세 조건 중 하나라도 충족 시 flush:
 * <ul>
 *   <li>5초 주기 스케줄러</li>
 *   <li>500건 누적</li>
 *   <li>5MB 누적</li>
 * </ul>
 *
 * flush 시 월별 인덱스({@code app-audit-YYYY.MM} 등) 기준으로 그룹화하여 인덱스당 한 번의
 * {@code bulkIndex} 호출로 전송한다.
 *
 * 그레이스풀 셧다운: {@link #drainOnShutdown} 가 남은 버퍼를 flush — 유실 최소화.
 * 모든 flush 실패는 삼키고 WARN 로그만 남긴다 (best-effort at-most-once).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogBatchBuffer {

    private static final int MAX_ITEMS = 500;
    private static final int MAX_BYTES = 5 * 1024 * 1024;    // 5MB
    private static final long FLUSH_INTERVAL_MS = 5_000L;

    private final ElasticsearchOperations elasticsearchOperations;
    private final LogMonthlyIndexNameResolver indexNameResolver;

    private final Object lock = new Object();
    private List<ApplicationLog> buffer = new ArrayList<>();
    private int bufferedBytes = 0;

    public void enqueue(ApplicationLog log) {
        if (log == null) {
            return;
        }
        List<ApplicationLog> toFlush = null;
        synchronized (lock) {
            buffer.add(log);
            bufferedBytes += estimateBytes(log);
            if (buffer.size() >= MAX_ITEMS || bufferedBytes >= MAX_BYTES) {
                toFlush = swapOut();
            }
        }
        if (toFlush != null) {
            flush(toFlush);
        }
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    public void periodicFlush() {
        List<ApplicationLog> toFlush;
        synchronized (lock) {
            if (buffer.isEmpty()) {
                return;
            }
            toFlush = swapOut();
        }
        flush(toFlush);
    }

    /**
     * 앱 종료 시 잔여 버퍼 flush. @PreDestroy 훅.
     */
    @PreDestroy
    public void drainOnShutdown() {
        List<ApplicationLog> toFlush;
        synchronized (lock) {
            if (buffer.isEmpty()) {
                return;
            }
            toFlush = swapOut();
        }
        log.info("LogBatchBuffer shutdown drain: {}건", toFlush.size());
        flush(toFlush);
    }

    private List<ApplicationLog> swapOut() {
        List<ApplicationLog> out = buffer;
        buffer = new ArrayList<>();
        bufferedBytes = 0;
        return out;
    }

    private void flush(List<ApplicationLog> logs) {
        Map<String, List<IndexQuery>> grouped = new LinkedHashMap<>();
        for (ApplicationLog log : logs) {
            try {
                ApplicationLogDocument doc = LogDocumentMapper.toDocument(log);
                String indexName = indexNameResolver.resolve(log);
                IndexQuery query = new IndexQueryBuilder()
                        .withId(doc.getId())
                        .withObject(doc)
                        .build();
                grouped.computeIfAbsent(indexName, k -> new ArrayList<>()).add(query);
            } catch (Exception e) {
                LogBatchBuffer.log.warn("로그 문서 매핑 실패, 드롭: requestId={}, err={}",
                        log.requestId(), e.getMessage());
            }
        }
        for (Map.Entry<String, List<IndexQuery>> entry : grouped.entrySet()) {
            try {
                elasticsearchOperations.bulkIndex(entry.getValue(), IndexCoordinates.of(entry.getKey()));
            } catch (Exception e) {
                LogBatchBuffer.log.warn("ES bulkIndex 실패 (index={}, 건수={}): {}",
                        entry.getKey(), entry.getValue().size(), e.getMessage());
            }
        }
    }

    private int estimateBytes(ApplicationLog log) {
        int total = 64; // overhead 대략치
        if (log.requestId() != null) total += log.requestId().length();
        if (log.payload() != null) {
            for (Map.Entry<String, Object> entry : log.payload().entrySet()) {
                total += entry.getKey().length();
                total += valueBytes(entry.getValue());
            }
        }
        return total;
    }

    private int valueBytes(Object v) {
        if (v == null) return 4;
        if (v instanceof String s) return s.length();
        if (v instanceof Number || v instanceof Boolean) return 16;
        return v.toString().length();
    }
}
