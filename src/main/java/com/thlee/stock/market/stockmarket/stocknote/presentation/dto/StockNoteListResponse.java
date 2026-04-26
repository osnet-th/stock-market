package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteListItemResult;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteListResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 기록 리스트 Response.
 */
public record StockNoteListResponse(
        List<Item> items,
        long totalCount,
        int page,
        int size
) {

    public static StockNoteListResponse from(StockNoteListResult r) {
        return new StockNoteListResponse(
                r.items().stream().map(Item::from).toList(),
                r.totalCount(),
                r.page(),
                r.size()
        );
    }

    public record Item(
            Long id,
            String stockCode,
            MarketType marketType,
            ExchangeCode exchangeCode,
            NoteDirection direction,
            BigDecimal changePercent,
            LocalDate noteDate,
            UserJudgment initialJudgment,
            String triggerTextSummary,
            List<TagPayload> tags,
            List<SnapshotSummary> snapshots,
            String judgmentResult,
            boolean verified
    ) {
        public static Item from(StockNoteListItemResult r) {
            StockNote n = r.note();
            StockNoteVerification v = r.verification();
            return new Item(
                    n.getId(), n.getStockCode(), n.getMarketType(), n.getExchangeCode(),
                    n.getDirection(), n.getChangePercent(), n.getNoteDate(),
                    n.getInitialJudgment(),
                    summarize(n.getTriggerText(), 120),
                    r.tags().stream()
                            .map(Item::toTag)
                            .toList(),
                    r.snapshots().stream().map(SnapshotSummary::from).toList(),
                    v == null ? null : v.getJudgmentResult().name(),
                    v != null
            );
        }

        private static TagPayload toTag(StockNoteTag t) {
            return new TagPayload(t.getTagSource(), t.getTagValue());
        }

        private static String summarize(String s, int maxLen) {
            if (s == null) {
                return null;
            }
            return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
        }
    }

    public record SnapshotSummary(String snapshotType, BigDecimal changePercent, String status) {
        public static SnapshotSummary from(StockNotePriceSnapshot s) {
            return new SnapshotSummary(s.getSnapshotType().name(), s.getChangePercent(), s.getStatus().name());
        }
    }
}