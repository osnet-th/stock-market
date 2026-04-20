package com.thlee.stock.market.stockmarket.logging.presentation.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 일별/주별 로그 건수 집계 응답.
 * {@code interval} 은 {@code day} 또는 {@code week} — window 에 따라 자동 선택.
 */
public record LogDailyCountResponse(
        String interval,
        List<DailyCount> counts
) {

    public record DailyCount(LocalDate date, long count) {
    }
}