package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.MonthlyIncomeEntity;

/**
 * MonthlyIncome Entity ↔ Domain 변환.
 */
public class MonthlyIncomeMapper {

    private MonthlyIncomeMapper() {
    }

    public static MonthlyIncomeEntity toEntity(MonthlyIncome domain) {
        return new MonthlyIncomeEntity(
                domain.getId(),
                domain.getUserId(),
                YearMonthConverter.toLocalDate(domain.getEffectiveFromMonth()),
                domain.getAmount(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static MonthlyIncome toDomain(MonthlyIncomeEntity entity) {
        return new MonthlyIncome(
                entity.getId(),
                entity.getUserId(),
                YearMonthConverter.toYearMonth(entity.getEffectiveFromMonth()),
                entity.getAmount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}