package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 기록 본체 Entity. Valuation / Fundamental 은 1:1 흡수 (설계 심화 A).
 */
@Entity
@Table(
        name = "stock_note",
        indexes = {
                @Index(name = "idx_stock_note_user_date", columnList = "user_id, note_date DESC"),
                @Index(name = "idx_stock_note_user_stock_date",
                        columnList = "user_id, stock_code, note_date DESC")
        }
)
@Getter
public class StockNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    private MarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_code", nullable = false, length = 10)
    private ExchangeCode exchangeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private NoteDirection direction;

    @Column(name = "change_percent", precision = 8, scale = 2)
    private BigDecimal changePercent;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(name = "trigger_text", columnDefinition = "TEXT")
    private String triggerText;

    @Column(name = "interpretation_text", columnDefinition = "TEXT")
    private String interpretationText;

    @Column(name = "risk_text", columnDefinition = "TEXT")
    private String riskText;

    @Column(name = "is_pre_reflected", nullable = false)
    private boolean preReflected;

    @Enumerated(EnumType.STRING)
    @Column(name = "initial_judgment", nullable = false, length = 30)
    private UserJudgment initialJudgment;

    // Valuation (흡수)
    @Column(name = "per", precision = 10, scale = 2)
    private BigDecimal per;

    @Column(name = "pbr", precision = 10, scale = 2)
    private BigDecimal pbr;

    @Column(name = "ev_ebitda", precision = 10, scale = 2)
    private BigDecimal evEbitda;

    @Enumerated(EnumType.STRING)
    @Column(name = "vs_average", length = 10)
    private VsAverageLevel vsAverage;

    // Fundamental Link (흡수)
    @Enumerated(EnumType.STRING)
    @Column(name = "revenue_impact", length = 10)
    private ImpactLevel revenueImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "profit_impact", length = 10)
    private ImpactLevel profitImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "cashflow_impact", length = 10)
    private ImpactLevel cashflowImpact;

    @Column(name = "is_one_time", nullable = false)
    private boolean oneTime;

    @Column(name = "is_structural", nullable = false)
    private boolean structural;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StockNoteEntity() {
    }

    public StockNoteEntity(Long id, Long userId, String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                           NoteDirection direction, BigDecimal changePercent, LocalDate noteDate,
                           String triggerText, String interpretationText, String riskText,
                           boolean preReflected, UserJudgment initialJudgment,
                           BigDecimal per, BigDecimal pbr, BigDecimal evEbitda, VsAverageLevel vsAverage,
                           ImpactLevel revenueImpact, ImpactLevel profitImpact, ImpactLevel cashflowImpact,
                           boolean oneTime, boolean structural,
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
        this.per = per;
        this.pbr = pbr;
        this.evEbitda = evEbitda;
        this.vsAverage = vsAverage;
        this.revenueImpact = revenueImpact;
        this.profitImpact = profitImpact;
        this.cashflowImpact = cashflowImpact;
        this.oneTime = oneTime;
        this.structural = structural;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}