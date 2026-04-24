package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.SimilarPatternResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/stock-notes/{id}/similar-patterns Response.
 */
public record SimilarPatternResponse(
        List<TagPayload> basisTags,
        List<MatchDto> matches,
        AggregateDto aggregate
) {
    public static SimilarPatternResponse from(SimilarPatternResult r) {
        return new SimilarPatternResponse(
                r.basisTags().stream()
                        .map(p -> new TagPayload(p.source(), p.value()))
                        .toList(),
                r.matches().stream().map(MatchDto::from).toList(),
                AggregateDto.from(r.aggregate())
        );
    }

    public record MatchDto(
            Long noteId,
            String stockCode,
            LocalDate noteDate,
            NoteDirection direction,
            BigDecimal d7ChangePercent,
            BigDecimal d30ChangePercent,
            JudgmentResult judgmentResult
    ) {
        public static MatchDto from(SimilarPatternResult.Match m) {
            return new MatchDto(m.noteId(), m.stockCode(), m.noteDate(), m.direction(),
                    m.d7ChangePercent(), m.d30ChangePercent(), m.judgmentResult());
        }
    }

    public record AggregateDto(
            int total,
            long correct,
            long wrong,
            long partial,
            BigDecimal avgD7Percent,
            BigDecimal avgD30Percent
    ) {
        public static AggregateDto from(SimilarPatternResult.Aggregate a) {
            return new AggregateDto(a.total(), a.correct(), a.wrong(), a.partial(),
                    a.avgD7Percent(), a.avgD30Percent());
        }
    }
}