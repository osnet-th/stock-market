package com.thlee.stock.market.stockmarket.salary.domain.repository;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 월급 변경 이력 Repository 포트.
 */
public interface MonthlyIncomeRepository {

    MonthlyIncome save(MonthlyIncome income);

    /** 해당 월의 변경 레코드(상속이 아닌 직접 레코드) */
    Optional<MonthlyIncome> findByUserIdAndEffectiveFromMonth(Long userId, YearMonth yearMonth);

    /**
     * 특정 월 시점의 유효 월급 (상속 포함).
     * {@code effectiveFromMonth <= targetMonth}인 레코드 중 가장 최근 것.
     */
    Optional<MonthlyIncome> findEffectiveAsOf(Long userId, YearMonth targetMonth);

    /**
     * 주어진 월 이하의 모든 변경 레코드.
     * 12개월 추이 계산 시 앱 메모리에서 롤링 포워드하기 위해 사용.
     * {@code effectiveFromMonth ASC}로 정렬된다.
     */
    List<MonthlyIncome> findAllUpTo(Long userId, YearMonth endMonth);

    /** 변경 레코드가 존재하는 월 목록 (최신 우선) */
    List<YearMonth> findDistinctMonths(Long userId);

    void deleteByUserIdAndEffectiveFromMonth(Long userId, YearMonth yearMonth);
}