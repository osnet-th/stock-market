package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SpendingItemSetJpaRepository extends JpaRepository<SpendingItemSetEntity, Long> {

    Optional<SpendingItemSetEntity> findByUserIdAndEffectiveFromMonth(Long userId, LocalDate effectiveFromMonth);

    /** 상속 조회 — {@code effective_from_month <= target} 중 최신 1건 */
    Optional<SpendingItemSetEntity> findFirstByUserIdAndEffectiveFromMonthLessThanEqualOrderByEffectiveFromMonthDesc(
            Long userId, LocalDate targetMonth);
}
