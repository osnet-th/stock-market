package com.thlee.stock.market.stockmarket.salary.domain.repository;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItemSet;

import java.time.YearMonth;
import java.util.Optional;

/**
 * 하위 항목 스냅샷 세트 Repository 포트.
 */
public interface SpendingItemSetRepository {

    /** 세트 저장 — 항목은 세트 단위 전체 교체로 저장된다. */
    SpendingItemSet save(SpendingItemSet itemSet);

    /** 해당 월의 세트(상속이 아닌 직접 레코드) */
    Optional<SpendingItemSet> findByUserIdAndEffectiveFromMonth(Long userId, YearMonth yearMonth);

    /**
     * 특정 월 시점의 유효 세트 (상속 포함).
     * {@code effectiveFromMonth <= targetMonth}인 세트 중 가장 최근 것.
     */
    Optional<SpendingItemSet> findEffectiveAsOf(Long userId, YearMonth targetMonth);
}
