package com.thlee.stock.market.stockmarket.stockevaluation.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 업종 일자별 지수 응답 DTO (차트용).
 * points는 과거→최신 오름차순. value는 raw 숫자(표시 형식은 프론트).
 */
@Getter
@Builder
public class IndustryIndexResponse {

    private final String code;          // 지수업종 코드 (4자리)
    private final List<IndexPoint> points;

    @Getter
    @Builder
    public static class IndexPoint {
        private final String date;      // YYYY-MM-DD
        private final String value;     // 업종 지수
    }
}
