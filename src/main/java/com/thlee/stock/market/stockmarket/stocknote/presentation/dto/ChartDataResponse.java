package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.ChartDataResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/stock-notes/by-stock/{stockCode}/chart Response.
 *
 * <p>prices 는 line 차트 데이터셋 소스, notes 는 scatter overlay 기록점.
 */
public record ChartDataResponse(
        String stockCode,
        List<PricePoint> prices,
        List<NotePoint> notes
) {
    public static ChartDataResponse from(ChartDataResult r) {
        return new ChartDataResponse(
                r.stockCode(),
                r.prices().stream().map(PricePoint::from).toList(),
                r.notes().stream().map(NotePoint::from).toList()
        );
    }

    public record PricePoint(LocalDate date, BigDecimal close, BigDecimal open, BigDecimal high, BigDecimal low) {
        public static PricePoint from(DailyPrice d) {
            return new PricePoint(d.date(), d.close(), d.open(), d.high(), d.low());
        }
    }

    public record NotePoint(
            Long noteId,
            LocalDate noteDate,
            NoteDirection direction,
            BigDecimal priceAtNote,
            BigDecimal changePercent,
            UserJudgment initialJudgment,
            String summary,
            boolean verified
    ) {
        public static NotePoint from(ChartDataResult.NotePoint n) {
            return new NotePoint(n.noteId(), n.noteDate(), n.direction(), n.priceAtNote(),
                    n.changePercent(), n.initialJudgment(), n.triggerTextSummary(), n.verified());
        }
    }
}