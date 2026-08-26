package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ChartPeriod;
import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 기간별(일/주/월봉) 가격 히스토리 응답.
 * points는 date ASC 정렬. 값은 도메인 {@link DailyPrice}를 표시용으로 변환한 것.
 */
@Getter
@Builder
public class PriceHistoryResponse {

    private final String stockCode;
    private final String period;
    private final List<PricePoint> points;

    @Getter
    @Builder
    public static class PricePoint {
        private final String date;    // yyyy-MM-dd
        private final String open;
        private final String high;
        private final String low;
        private final String close;
        private final Long volume;
    }

    public static PriceHistoryResponse from(String stockCode, ChartPeriod period, List<DailyPrice> prices) {
        List<PricePoint> points = prices.stream()
            .map(p -> PricePoint.builder()
                .date(p.date().toString())
                .open(p.open() != null ? p.open().toPlainString() : null)
                .high(p.high() != null ? p.high().toPlainString() : null)
                .low(p.low() != null ? p.low().toPlainString() : null)
                .close(p.close() != null ? p.close().toPlainString() : null)
                .volume(p.volume())
                .build())
            .toList();
        return PriceHistoryResponse.builder()
            .stockCode(stockCode)
            .period(period.getCode())
            .points(points)
            .build();
    }
}
