package com.thlee.stock.market.stockmarket.stockevaluation.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 종목추정실적 응답 DTO.
 * output1~4를 섹션 단위 표로 묶어 전달한다. (KIS 라벨 비신뢰 → 원시 키 컬럼)
 */
@Getter
@Builder
public class EstimatePerformResponse {

    private final List<Section> sections;

    @Getter
    @Builder
    public static class Section {
        private final String title;
        private final KisTableResponse table;
    }
}
