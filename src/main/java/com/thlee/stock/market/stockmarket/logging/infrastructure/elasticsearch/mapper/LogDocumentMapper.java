package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.mapper;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.document.ApplicationLogDocument;

import java.util.UUID;

/**
 * ApplicationLog(Domain) → ApplicationLogDocument(ES) 변환 Mapper.
 *
 * ID 생성 규칙:
 *  - ERROR: {@code {requestId}-{exceptionClass}} — 동일 요청의 동일 예외 중복 적재 방지
 *    (AOP @AfterThrowing + GlobalExceptionHandler 이중 수집 구조에서 자연 dedup)
 *  - 그 외: UUID
 */
public class LogDocumentMapper {

    private LogDocumentMapper() {
    }

    public static ApplicationLogDocument toDocument(ApplicationLog log) {
        return new ApplicationLogDocument(
                resolveId(log),
                log.timestamp(),
                log.domain().getIndexSuffix(),
                log.userId(),
                log.requestId(),
                log.payload(),
                log.truncated(),
                log.originalSize()
        );
    }

    private static String resolveId(ApplicationLog log) {
        if (log.domain() == LogDomain.ERROR) {
            Object exceptionClass = log.payload() != null ? log.payload().get("exceptionClass") : null;
            if (log.requestId() != null && exceptionClass != null) {
                return log.requestId() + "-" + exceptionClass;
            }
        }
        return UUID.randomUUID().toString();
    }
}