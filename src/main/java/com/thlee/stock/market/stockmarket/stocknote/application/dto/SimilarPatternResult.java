package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 태그 조합 유사 패턴 매칭 결과.
 */
public record SimilarPatternResult(
        List<TagPair> basisTags,
        List<Match> matches,
        Aggregate aggregate
) {
    public record TagPair(String source, String value) { }

    public record Match(
            Long noteId,
            String stockCode,
            LocalDate noteDate,
            NoteDirection direction,
            BigDecimal d7ChangePercent,
            BigDecimal d30ChangePercent,
            JudgmentResult judgmentResult
    ) { }

    public record Aggregate(
            int total,
            long correct,
            long wrong,
            long partial,
            BigDecimal avgD7Percent,
            BigDecimal avgD30Percent,
            long upAfter1W,
            long downAfter1W,
            long upAfter1M,
            long downAfter1M
    ) { }
}