package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 기록 본체.
 *
 * <p>등락 시점의 10가지 판단 축을 한 엔티티로 보관한다. Valuation / FundamentalImpact 영향도는
 * 1:1 관계로 단순하므로 별도 테이블 대신 본체 컬럼으로 흡수하되, 도메인 모델에서는 두 그룹을
 * {@link Valuation} / {@link FundamentalImpact} VO 로 묶어 swap 위험을 차단한다 (DB 컬럼은 평면 유지).
 *
 * <p>Entity 연관관계는 두지 않고 {@code stockCode / marketType / exchangeCode} 값 참조만 보유한다.
 */
@Getter
public class StockNote {

    /** TEXT 필드 길이 상한 (보안/저장 보호) */
    public static final int TEXT_MAX_LENGTH = 4000;

    private Long id;
    private final Long userId;
    private final String stockCode;
    private final MarketType marketType;
    private final ExchangeCode exchangeCode;
    private final NoteDirection direction;
    private final BigDecimal changePercent;
    private final LocalDate noteDate;

    private String triggerText;
    private String interpretationText;
    private String riskText;
    private boolean preReflected;
    private UserJudgment initialJudgment;

    private Valuation valuation;
    private FundamentalImpact fundamentalImpact;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public StockNote(Long id, Long userId, String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                     NoteDirection direction, BigDecimal changePercent, LocalDate noteDate,
                     String triggerText, String interpretationText, String riskText,
                     boolean preReflected, UserJudgment initialJudgment,
                     Valuation valuation, FundamentalImpact fundamentalImpact,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.stockCode = stockCode;
        this.marketType = marketType;
        this.exchangeCode = exchangeCode;
        this.direction = direction;
        this.changePercent = changePercent;
        this.noteDate = noteDate;
        this.triggerText = triggerText;
        this.interpretationText = interpretationText;
        this.riskText = riskText;
        this.preReflected = preReflected;
        this.initialJudgment = initialJudgment;
        this.valuation = valuation == null ? Valuation.empty() : valuation;
        this.fundamentalImpact = fundamentalImpact == null ? FundamentalImpact.empty() : fundamentalImpact;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 기록 생성 팩토리.
     *
     * @param today noteDate 미래 검증 기준 (테스트 주입 목적)
     */
    public static StockNote create(Long userId, String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                                   NoteDirection direction, BigDecimal changePercent, LocalDate noteDate, LocalDate today,
                                   String triggerText, String interpretationText, String riskText,
                                   boolean preReflected, UserJudgment initialJudgment,
                                   Valuation valuation, FundamentalImpact fundamentalImpact) {
        requireNonNull(userId, "userId");
        requireNonBlank(stockCode, "stockCode");
        requireNonNull(marketType, "marketType");
        requireNonNull(exchangeCode, "exchangeCode");
        requireNonNull(direction, "direction");
        requireNonNull(noteDate, "noteDate");
        requireNonNull(today, "today");
        requireNonNull(initialJudgment, "initialJudgment");
        if (noteDate.isAfter(today)) {
            throw new IllegalArgumentException("미래 날짜로 기록할 수 없습니다.");
        }
        requireTextWithin(triggerText, "triggerText");
        requireTextWithin(interpretationText, "interpretationText");
        requireTextWithin(riskText, "riskText");
        LocalDateTime now = LocalDateTime.now();
        return new StockNote(null, userId, stockCode, marketType, exchangeCode, direction, changePercent, noteDate,
                triggerText, interpretationText, riskText, preReflected, initialJudgment,
                valuation == null ? Valuation.empty() : valuation,
                fundamentalImpact == null ? FundamentalImpact.empty() : fundamentalImpact,
                now, now);
    }

    /**
     * 본문 및 분석 항목 수정.
     * <p>잠금 여부는 application 계층에서 {@link StockNoteVerification} 존재 여부로 사전 판정한 뒤 호출한다.
     * 본 도메인 메서드는 값 검증만 수행한다.
     */
    public void updateBody(String triggerText, String interpretationText, String riskText,
                           boolean preReflected, UserJudgment initialJudgment,
                           Valuation valuation, FundamentalImpact fundamentalImpact) {
        requireNonNull(initialJudgment, "initialJudgment");
        requireTextWithin(triggerText, "triggerText");
        requireTextWithin(interpretationText, "interpretationText");
        requireTextWithin(riskText, "riskText");
        this.triggerText = triggerText;
        this.interpretationText = interpretationText;
        this.riskText = riskText;
        this.preReflected = preReflected;
        this.initialJudgment = initialJudgment;
        this.valuation = valuation == null ? Valuation.empty() : valuation;
        this.fundamentalImpact = fundamentalImpact == null ? FundamentalImpact.empty() : fundamentalImpact;
        this.updatedAt = LocalDateTime.now();
    }

    /** 저장 후 id 주입용 (RepositoryImpl 내부 재구성 전용) */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    // -------- 위임 getter (Mapper / Response DTO 호환 — VO 분리 이전 평면 호출 패턴 유지) --------

    public BigDecimal getPer() { return valuation.per(); }
    public BigDecimal getPbr() { return valuation.pbr(); }
    public BigDecimal getEvEbitda() { return valuation.evEbitda(); }
    public VsAverageLevel getVsAverage() { return valuation.vsAverage(); }
    public ImpactLevel getRevenueImpact() { return fundamentalImpact.revenueImpact(); }
    public ImpactLevel getProfitImpact() { return fundamentalImpact.profitImpact(); }
    public ImpactLevel getCashflowImpact() { return fundamentalImpact.cashflowImpact(); }
    public boolean isOneTime() { return fundamentalImpact.oneTime(); }
    public boolean isStructural() { return fundamentalImpact.structural(); }

    private static void requireNonNull(Object v, String name) {
        if (v == null) {
            throw new IllegalArgumentException(name + " 는 필수입니다.");
        }
    }

    private static void requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " 는 필수입니다.");
        }
    }

    private static void requireTextWithin(String v, String name) {
        if (v != null && v.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException(name + " 길이는 " + TEXT_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }
}