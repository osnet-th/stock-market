package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.DashboardResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 KPI 집계 전용 Repository 포트.
 * 일반 CRUD 가 아닌 화면 단위 집계 쿼리만 보유 — application 의 EntityManager 직접 사용을 제거하기 위함
 * (ARCHITECTURE.md Section 4/5 포트-어댑터 패턴).
 *
 * <p>반환 타입에 application DTO ({@link DashboardResult.HitRate} / {@link DashboardResult.TagComboEntry}) 를
 * 직접 사용한다. 도메인이 application 을 의존하는 역방향이지만, 해당 DTO 는 외부 노출 contract 가 아닌
 * 화면별 집계 결과 record 라 누수 위험 없음 (실용적 타협).
 */
public interface StockNoteDashboardRepository {

    /** 사용자의 [from, to] 범위 noteDate 기록 수. */
    long countByUserAndNoteDateBetween(Long userId, LocalDate from, LocalDate to);

    /** 검증 결과 분포 (CORRECT/WRONG/PARTIAL/total). */
    DashboardResult.HitRate aggregateHitRate(Long userId);

    /** 검증되지 않은 기록 수. */
    long countPendingVerification(Long userId);

    /** CHARACTER 태그 분포 — value &rarr; count, count DESC. */
    Map<String, Long> aggregateCharacterDistribution(Long userId);

    /** 상위 N 개 태그 조합 — combo (List&lt;source::value&gt;) + count DESC. */
    List<DashboardResult.TagComboEntry> aggregateTopTagCombos(Long userId, int limit);
}