package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.DepositHistory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class DepositHistoryResponse {
    private final Long id;
    private final Long portfolioItemId;
    private final LocalDate depositDate;
    private final BigDecimal amount;
    private final BigDecimal units;
    private final String memo;
    private final LocalDateTime createdAt;

    private DepositHistoryResponse(Long id, Long portfolioItemId, LocalDate depositDate,
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

    public static DepositHistoryResponse from(DepositHistory history) {
        return new DepositHistoryResponse(
                history.getId(),
                history.getPortfolioItemId(),
                history.getDepositDate(),
                history.getAmount(),
                history.getUnits(),
                history.getMemo(),
                history.getCreatedAt()
        );
    }
}