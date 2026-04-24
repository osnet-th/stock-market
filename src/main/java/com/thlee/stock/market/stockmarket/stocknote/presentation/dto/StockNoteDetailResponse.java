package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteDetailResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기록 상세 Response. note + tags + snapshots + verification 동봉.
 */
public record StockNoteDetailResponse(
        NoteDto note,
        List<TagPayload> tags,
        List<SnapshotDto> snapshots,
        VerificationDto verification
) {

    public static StockNoteDetailResponse from(StockNoteDetailResult r) {
        return new StockNoteDetailResponse(
                NoteDto.from(r.note()),
                r.tags().stream().map(StockNoteDetailResponse::toTag).toList(),
                r.snapshots().stream().map(SnapshotDto::from).toList(),
                r.verification() == null ? null : VerificationDto.from(r.verification())
        );
    }

    private static TagPayload toTag(StockNoteTag t) {
        return new TagPayload(t.getTagSource(), t.getTagValue());
    }

    public record NoteDto(
            Long id,
            Long userId,
            String stockCode,
            MarketType marketType,
            ExchangeCode exchangeCode,
            NoteDirection direction,
            BigDecimal changePercent,
            LocalDate noteDate,
            String triggerText,
            String interpretationText,
            String riskText,
            boolean preReflected,
            UserJudgment initialJudgment,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal evEbitda,
            VsAverageLevel vsAverage,
            ImpactLevel revenueImpact,
            ImpactLevel profitImpact,
            ImpactLevel cashflowImpact,
            boolean oneTime,
            boolean structural,
            boolean locked,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static NoteDto from(StockNote n) {
            return new NoteDto(
                    n.getId(), n.getUserId(), n.getStockCode(), n.getMarketType(), n.getExchangeCode(),
                    n.getDirection(), n.getChangePercent(), n.getNoteDate(),
                    n.getTriggerText(), n.getInterpretationText(), n.getRiskText(),
                    n.isPreReflected(), n.getInitialJudgment(),
                    n.getPer(), n.getPbr(), n.getEvEbitda(), n.getVsAverage(),
                    n.getRevenueImpact(), n.getProfitImpact(), n.getCashflowImpact(),
                    n.isOneTime(), n.isStructural(),
                    /* locked - Detail Response 에서는 상위 래퍼가 verification != null 여부로 결정 */
                    false,
                    n.getCreatedAt(), n.getUpdatedAt()
            );
        }
    }

    public record SnapshotDto(
            String snapshotType,
            LocalDate priceDate,
            BigDecimal closePrice,
            BigDecimal changePercent,
            String status,
            String failureReason,
            int retryCount,
            LocalDateTime capturedAt
    ) {
        public static SnapshotDto from(StockNotePriceSnapshot s) {
            return new SnapshotDto(
                    s.getSnapshotType().name(),
                    s.getPriceDate(), s.getClosePrice(), s.getChangePercent(),
                    s.getStatus().name(), s.getFailureReason(), s.getRetryCount(), s.getCapturedAt()
            );
        }
    }

    public record VerificationDto(
            Long id,
            String judgmentResult,
            String verificationNote,
            LocalDateTime verifiedAt
    ) {
        public static VerificationDto from(StockNoteVerification v) {
            return new VerificationDto(v.getId(), v.getJudgmentResult().name(),
                    v.getVerificationNote(), v.getVerifiedAt());
        }
    }
}