package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.SpendingConfigEntity;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * SpendingConfig Entity ↔ Domain 변환.
 * YearMonth ↔ LocalDate(매월 1일) 변환의 단일 지점.
 */
public class SpendingConfigMapper {

    private SpendingConfigMapper() {
    }

    public static SpendingConfigEntity toEntity(SpendingConfig domain) {
        return new SpendingConfigEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getCategory(),
                toLocalDate(domain.getEffectiveFromMonth()),
                domain.getAmount(),
                domain.getMemo(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static SpendingConfig toDomain(SpendingConfigEntity entity) {
        return new SpendingConfig(
                entity.getId(),
                entity.getUserId(),
                entity.getCategory(),
                toYearMonth(entity.getEffectiveFromMonth()),
                entity.getAmount(),
                entity.getMemo(),
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