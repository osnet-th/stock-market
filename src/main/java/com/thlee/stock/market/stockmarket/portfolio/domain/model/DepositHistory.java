package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class DepositHistory {
    private Long id;
    private Long portfolioItemId;
    private LocalDate depositDate;
    private BigDecimal amount;
    private BigDecimal units;
    private String memo;
    private LocalDateTime createdAt;

    /**
     * 재구성용 생성자 (Repository 조회 시)
     */
    public DepositHistory(Long id, Long portfolioItemId, LocalDate depositDate,
                          BigDecimal amount, BigDecimal units,
                          String memo, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.depositDate = depositDate;
        this.amount = amount;
        this.units = units;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    /**
     * 새 납입이력 생성
     */
    public static DepositHistory create(Long portfolioItemId, LocalDate depositDate,
                                         BigDecimal amount, BigDecimal units, String memo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("납입 금액은 0보다 커야 합니다.");
        }
        return new DepositHistory(null, portfolioItemId,
                depositDate != null ? depositDate : LocalDate.now(),
                amount, units, memo, LocalDateTime.now());
    }

    /**
     * 납입이력 수정
     */
    public void update(LocalDate depositDate, BigDecimal amount, BigDecimal units, String memo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("납입 금액은 0보다 커야 합니다.");
        }
        if (depositDate != null) {
            this.depositDate = depositDate;
        }
        this.amount = amount;
        this.units = units;
        this.memo = memo;
    }
}