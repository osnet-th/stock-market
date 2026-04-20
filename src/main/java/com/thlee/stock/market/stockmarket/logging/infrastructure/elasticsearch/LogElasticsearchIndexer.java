package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.domain.service.LogIndexPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ES 로그 인덱싱 어댑터 — {@link LogIndexPort} 구현.
 *
 * 실제 ES 쓰기는 {@link LogBatchBuffer} 가 5s/500건/5MB 기준으로 배치 flush 하며,
 * 본 어댑터는 port 입구에서 버퍼로 enqueue 만 수행한다. 월별 인덱스 라우팅은 buffer 내부에서
 * 처리되므로 여기서 별도 변환이 필요 없다.
 *
 * ES 장애는 buffer 의 flush 단계에서 삼켜져 WARN 으로만 노출된다 (best-effort at-most-once).
 */
@Component
@RequiredArgsConstructor
public class LogElasticsearchIndexer implements LogIndexPort {

    private final LogBatchBuffer logBatchBuffer;

    @Override
    public void save(ApplicationLog log) {
        logBatchBuffer.enqueue(log);
    }
}