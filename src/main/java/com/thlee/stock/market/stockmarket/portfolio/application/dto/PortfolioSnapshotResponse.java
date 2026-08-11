package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioSnapshot;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class PortfolioSnapshotResponse {
    private final LocalDate snapshotDate;
    private final BigDecimal totalEvaluated;
    private final BigDecimal totalInvested;

    private PortfolioSnapshotResponse(LocalDate snapshotDate, BigDecimal totalEvaluated,
                                      BigDecimal totalInvested) {
        this.snapshotDate = snapshotDate;
        this.totalEvaluated = totalEvaluated;
        this.totalInvested = totalInvested;
    }

    public static PortfolioSnapshotResponse from(PortfolioSnapshot snapshot) {
        return new PortfolioSnapshotResponse(
                snapshot.getSnapshotDate(),
                snapshot.getTotalEvaluated(),
                snapshot.getTotalInvested()
        );
    }
}
