package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItem;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItemSet;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.SpendingItemEntity;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.SpendingItemSetEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpendingItemSet Entity ↔ Domain 변환.
 */
public class SpendingItemSetMapper {

    private SpendingItemSetMapper() {
    }

    public static SpendingItemSetEntity toHeaderEntity(SpendingItemSet domain) {
        return new SpendingItemSetEntity(
                domain.getId(),
                domain.getUserId(),
                YearMonthConverter.toLocalDate(domain.getEffectiveFromMonth()),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    /** 항목 행 생성 — 세트 재저장 시 통째로 재삽입되므로 id는 항상 null */
    public static SpendingItemEntity toItemEntity(SpendingItem item, Long setId, LocalDateTime createdAt) {
        return new SpendingItemEntity(
                null,
                setId,
                item.getCategory(),
                item.getName(),
                item.getAmount(),
                item.isFixed(),
                item.getSortOrder(),
                createdAt
        );
    }

    public static SpendingItemSet toDomain(SpendingItemSetEntity header, List<SpendingItemEntity> items) {
        return new SpendingItemSet(
                header.getId(),
                header.getUserId(),
                YearMonthConverter.toYearMonth(header.getEffectiveFromMonth()),
                items.stream().map(SpendingItemSetMapper::toDomainItem).collect(Collectors.toList()),
                header.getCreatedAt(),
                header.getUpdatedAt()
        );
    }

    private static SpendingItem toDomainItem(SpendingItemEntity entity) {
        return new SpendingItem(
                entity.getId(),
                entity.getCategory(),
                entity.getName(),
                entity.getAmount(),
                entity.isFixed(),
                entity.getSortOrder()
        );
    }
}
