package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일자별 포트폴리오 스냅샷 (#110)
 *
 * 자산 추이 차트의 데이터 소스. (userId, snapshotDate) 하나당 한 건이며 같은 날 재저장은 덮어쓴다.
 */
@Getter
public class PortfolioSnapshot {
    private Long id;
    private final Long userId;
    private final LocalDate snapshotDate;
    private BigDecimal totalEvaluated;
    private BigDecimal totalInvested;
    private final LocalDateTime createdAt;

    public PortfolioSnapshot(Long id, Long userId, LocalDate snapshotDate,
                             BigDecimal totalEvaluated, BigDecimal totalInvested,
                             LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.snapshotDate = snapshotDate;
        this.totalEvaluated = totalEvaluated;
        this.totalInvested = totalInvested;
        this.createdAt = createdAt;
    }

    public static PortfolioSnapshot create(Long userId, LocalDate snapshotDate,
                                           BigDecimal totalEvaluated, BigDecimal totalInvested) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (snapshotDate == null) {
            throw new IllegalArgumentException("스냅샷 기준일은 필수입니다.");
        }
        validateAmount(totalEvaluated, "총 평가액");
        validateAmount(totalInvested, "총 투자원금");
        return new PortfolioSnapshot(null, userId, snapshotDate, totalEvaluated, totalInvested,
                LocalDateTime.now());
    }

    /**
     * 같은 날 재저장 — 금액만 갱신한다 (id·기준일 유지).
     */
    public void refresh(BigDecimal totalEvaluated, BigDecimal totalInvested) {
        validateAmount(totalEvaluated, "총 평가액");
        validateAmount(totalInvested, "총 투자원금");
        this.totalEvaluated = totalEvaluated;
        this.totalInvested = totalInvested;
    }

    private static void validateAmount(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + "은 0 이상이어야 합니다.");
        }
    }
}
