package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 재무 타임라인 조회 항목
 */
public enum TimelineItem {
    ACCOUNTS,   // 요약 재무계정
    INDICES,    // 재무지표 4분류
    SHARES,     // 발행 주식수
    FCF,        // 잉여현금흐름 (전체 재무제표에서 파생)
    DETAILS     // 전체 재무제표 세부 계정
}
