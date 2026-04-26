package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.FundamentalImpact;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteCustomTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.Valuation;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.StockNoteCustomTagEntity;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.StockNoteEntity;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.StockNotePriceSnapshotEntity;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.StockNoteTagEntity;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.StockNoteVerificationEntity;

/**
 * stocknote 도메인의 Entity ↔ Domain 변환을 모아 둔다.
 * 5개 도메인 모델 × 2 방향 = 10개의 static 메서드.
 */
public final class StockNoteMapper {

    private StockNoteMapper() {
    }

    // -------- StockNote (본체) --------
    public static StockNoteEntity toEntity(StockNote d) {
        return new StockNoteEntity(
                d.getId(), d.getUserId(), d.getStockCode(), d.getMarketType(), d.getExchangeCode(),
                d.getDirection(), d.getChangePercent(), d.getNoteDate(),
                d.getTriggerText(), d.getInterpretationText(), d.getRiskText(),
                d.isPreReflected(), d.getInitialJudgment(),
                d.getPer(), d.getPbr(), d.getEvEbitda(), d.getVsAverage(),
                d.getRevenueImpact(), d.getProfitImpact(), d.getCashflowImpact(),
                d.isOneTime(), d.isStructural(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static StockNote toDomain(StockNoteEntity e) {
        Valuation valuation = new Valuation(e.getPer(), e.getPbr(), e.getEvEbitda(), e.getVsAverage());
        FundamentalImpact fundamentalImpact = new FundamentalImpact(
                e.getRevenueImpact(), e.getProfitImpact(), e.getCashflowImpact(),
                e.isOneTime(), e.isStructural());
        return new StockNote(
                e.getId(), e.getUserId(), e.getStockCode(), e.getMarketType(), e.getExchangeCode(),
                e.getDirection(), e.getChangePercent(), e.getNoteDate(),
                e.getTriggerText(), e.getInterpretationText(), e.getRiskText(),
                e.isPreReflected(), e.getInitialJudgment(),
                valuation, fundamentalImpact,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    // -------- StockNoteTag --------
    public static StockNoteTagEntity toEntity(StockNoteTag d) {
        return new StockNoteTagEntity(d.getId(), d.getNoteId(), d.getUserId(), d.getTagSource(), d.getTagValue());
    }

    public static StockNoteTag toDomain(StockNoteTagEntity e) {
        return new StockNoteTag(e.getId(), e.getNoteId(), e.getUserId(), e.getTagSource(), e.getTagValue());
    }

    // -------- StockNotePriceSnapshot --------
    public static StockNotePriceSnapshotEntity toEntity(StockNotePriceSnapshot d) {
        return new StockNotePriceSnapshotEntity(
                d.getId(), d.getNoteId(), d.getSnapshotType(),
                d.getPriceDate(), d.getClosePrice(), d.getChangePercent(),
                d.getStatus(), d.getFailureReason(), d.getRetryCount(), d.getCapturedAt()
        );
    }

    public static StockNotePriceSnapshot toDomain(StockNotePriceSnapshotEntity e) {
        return new StockNotePriceSnapshot(
                e.getId(), e.getNoteId(), e.getSnapshotType(),
                e.getPriceDate(), e.getClosePrice(), e.getChangePercent(),
                e.getStatus(), e.getFailureReason(), e.getRetryCount(), e.getCapturedAt()
        );
    }

    // -------- StockNoteVerification --------
    public static StockNoteVerificationEntity toEntity(StockNoteVerification d) {
        return new StockNoteVerificationEntity(
                d.getId(), d.getNoteId(), d.getJudgmentResult(), d.getVerificationNote(), d.getVerifiedAt()
        );
    }

    public static StockNoteVerification toDomain(StockNoteVerificationEntity e) {
        return new StockNoteVerification(
                e.getId(), e.getNoteId(), e.getJudgmentResult(), e.getVerificationNote(), e.getVerifiedAt()
        );
    }

    // -------- StockNoteCustomTag --------
    public static StockNoteCustomTagEntity toEntity(StockNoteCustomTag d) {
        return new StockNoteCustomTagEntity(d.getId(), d.getUserId(), d.getTagValue(), d.getUsageCount());
    }

    public static StockNoteCustomTag toDomain(StockNoteCustomTagEntity e) {
        return new StockNoteCustomTag(e.getId(), e.getUserId(), e.getTagValue(), e.getUsageCount());
    }
}