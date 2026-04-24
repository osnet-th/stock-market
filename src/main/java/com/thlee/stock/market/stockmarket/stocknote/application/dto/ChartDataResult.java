package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 종목 차트 데이터 (일봉 + 기록점).
 *
 * <p>{@code priceAtNote} 는 AT_NOTE 스냅샷의 종가로, scatter y 축 좌표로 사용된다
 * (심화 권고 22: 원 plan 누락 항목).
 */
public record ChartDataResult(
        String stockCode,
        List<DailyPrice> prices,
        List<NotePoint> notes
) {
    public record NotePoint(
            Long noteId,
            LocalDate noteDate,
            NoteDirection direction,
            BigDecimal priceAtNote,
            BigDecimal changePercent,
            UserJudgment initialJudgment,
            String triggerTextSummary,
            boolean verified
    ) { }
}