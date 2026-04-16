package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.MonthlyIncomeEntity;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * MonthlyIncome Entity ↔ Domain 변환.
 * YearMonth ↔ LocalDate(매월 1일) 변환의 단일 지점.
 */
public class MonthlyIncomeMapper {

    private MonthlyIncomeMapper() {
    }

    public static MonthlyIncomeEntity toEntity(MonthlyIncome domain) {
        return new MonthlyIncomeEntity(
                domain.getId(),
                domain.getUserId(),
                toLocalDate(domain.getEffectiveFromMonth()),
                domain.getAmount(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static MonthlyIncome toDomain(MonthlyIncomeEntity entity) {
        return new MonthlyIncome(
                entity.getId(),
                entity.getUserId(),
                toYearMonth(entity.getEffectiveFromMonth()),
                entity.getAmount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static LocalDate toLocalDate(YearMonth ym) {
        return ym == null ? null : ym.atDay(1);
    }

    public static YearMonth toYearMonth(LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }
}