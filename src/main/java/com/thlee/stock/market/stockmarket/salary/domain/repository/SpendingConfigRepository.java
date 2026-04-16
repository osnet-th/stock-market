package com.thlee.stock.market.stockmarket.salary.domain.repository;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 카테고리별 지출 변경 이력 Repository 포트.
 */
public interface SpendingConfigRepository {

    SpendingConfig save(SpendingConfig config);

    /** 해당 월·카테고리의 변경 레코드(상속이 아닌 직접 레코드) */
    Optional<SpendingConfig> findByUserIdAndCategoryAndEffectiveFromMonth(
            Long userId, SpendingCategory category, YearMonth yearMonth);

    /**
     * 특정 월 시점의 카테고리별 유효 지출 (상속 포함).
     * 각 카테고리당 0 또는 1건. PostgreSQL {@code DISTINCT ON (category)}로 구현.
     */
    List<SpendingConfig> findEffectiveAsOf(Long userId, YearMonth targetMonth);

    /**
     * 주어진 월 이하의 모든 변경 레코드.
     * 12개월 추이 계산 시 앱 메모리에서 롤링 포워드하기 위해 사용.
     * {@code category ASC, effectiveFromMonth ASC}로 정렬된다.
     */
    List<SpendingConfig> findAllUpTo(Long userId, YearMonth endMonth);

    /** 변경 레코드가 존재하는 월 목록 (최신 우선) */
    List<YearMonth> findDistinctMonths(Long userId);

    void deleteByUserIdAndCategoryAndEffectiveFromMonth(
            Long userId, SpendingCategory category, YearMonth yearMonth);
}