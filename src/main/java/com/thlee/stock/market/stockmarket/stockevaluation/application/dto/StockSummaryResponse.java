package com.thlee.stock.market.stockmarket.stockevaluation.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 종목 요약 카드 응답 DTO (주식기본조회 정제값).
 * 평가 화면 최상단 카드에 노출된다.
 */
@Getter
@Builder
public class StockSummaryResponse {

    private final String stockCode;
    private final String stockName;        // 종목명 (약어명)
    private final String englishName;      // 영문명
    private final String stdCode;          // 표준코드(ISIN)
    private final String market;           // 코스피 / 코스닥 / 기타
    private final String industry;         // 업종(표준산업분류)
    private final String industryIndexCode; // 지수업종 코드 (4자리, 업종지수 조회용)
    private final String industryIndexName; // 지수업종명 (예: 전기,전자)
    private final String closePrice;       // 당일 종가 (콤마)
    private final String prevClosePrice;   // 전일 종가 (콤마)
    private final boolean kospi200;        // 코스피200 편입 여부
    private final boolean managed;         // 관리종목 여부
    private final boolean tradingHalted;   // 거래정지 여부
    private final List<SummaryItem> details; // 상장주식수/자본금/액면가/결산월/상장일/외국인한도

    @Getter
    @Builder
    public static class SummaryItem {
        private final String label;
        private final String value;
    }
}
