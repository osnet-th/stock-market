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
                log.originalSize(),
                extractStatus(log),
                extractExceptionClass(log)
        );
    }

    private static String resolveId(ApplicationLog log) {
        if (log.domain() == LogDomain.ERROR) {
            Object exceptionClass = extractExceptionClass(log);
            if (log.requestId() != null && exceptionClass != null) {
                return log.requestId() + "-" + exceptionClass;
            }
        }
        return UUID.randomUUID().toString();
    }

    /**
     * payload.status 를 최상위 {@code status} 로 승격 (검색 필터용).
     * AUDIT payload 에만 존재. 문자열/정수 모두 허용.
     */
    private static Integer extractStatus(ApplicationLog log) {
        if (log.payload() == null) {
            return null;
        }
        Object value = log.payload().get("status");
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * payload.exceptionClass 를 최상위 {@code exceptionClass} 로 승격 (검색 필터용).
     * ERROR payload 에만 존재.
     */
    private static String extractExceptionClass(ApplicationLog log) {
        if (log.payload() == null) {
            return null;
        }
        Object value = log.payload().get("exceptionClass");
        return value instanceof String s ? s : null;
    }
}