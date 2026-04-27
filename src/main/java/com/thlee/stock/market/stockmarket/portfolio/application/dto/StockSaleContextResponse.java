package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 매도 모달 진입 시 자동 입력 컨텍스트.
 * - currentPriceKrw: 현재가(원) — null이면 KIS 조회 실패
 * - currentPriceOriginal: 현재가(종목 통화) — KRW이면 동일
 * - currency: 종목 통화코드
 * - fxRate: 매도일 환율(KRW이면 1, null이면 자동 조회 실패 — 사용자 직접 입력 필요)
 * - totalAsset: 매도 시점 사용자 전체 자산 KRW 평가금액(자산 기여율 계산 기준)
 */
@Getter
@RequiredArgsConstructor
public class StockSaleContextResponse {
    private final BigDecimal currentPriceKrw;
    private final BigDecimal currentPriceOriginal;
    private final String currency;
    private final BigDecimal fxRate;
    private final BigDecimal totalAsset;
}