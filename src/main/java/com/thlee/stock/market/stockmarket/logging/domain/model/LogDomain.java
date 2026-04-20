package com.thlee.stock.market.stockmarket.logging.domain.model;

/**
 * 애플리케이션 로그 도메인 구분.
 *
 * AUDIT    — Controller 진입/응답 감사 (URI, userId, status, durationMs)
 * ERROR    — 예외/에러 (exceptionClass, message, stacktrace)
 * BUSINESS — 명시적 비즈니스 이벤트 (포트폴리오 변경, 챗봇 질의 등)
 */
public enum LogDomain {
    AUDIT("audit"),
    ERROR("error"),
    BUSINESS("business");

    private final String indexSuffix;

    LogDomain(String indexSuffix) {
        this.indexSuffix = indexSuffix;
    }

    public String getIndexSuffix() {
        return indexSuffix;
    }
}