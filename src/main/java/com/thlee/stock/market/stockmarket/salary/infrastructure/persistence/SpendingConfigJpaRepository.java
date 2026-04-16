package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpendingConfigJpaRepository extends JpaRepository<SpendingConfigEntity, Long> {

    Optional<SpendingConfigEntity> findByUserIdAndCategoryAndEffectiveFromMonth(
            Long userId, SpendingCategory category, LocalDate effectiveFromMonth);

    /**
     * 카테고리별 상속 조회. PostgreSQL {@code DISTINCT ON (category)}를 사용한다.
     * 인덱스 {@code (user_id, category, effective_from_month DESC)}가 Index Scan을 제공한다.
     */
    @Query(value = """
            SELECT DISTINCT ON (s.category)
                   s.id, s.user_id, s.category, s.effective_from_month,
                   s.amount, s.memo, s.created_at, s.updated_at
            FROM spending_config s
            WHERE s.user_id = :userId
              AND s.effective_from_month <= :targetMonth
            ORDER BY s.category, s.effective_from_month DESC
            """, nativeQuery = true)
    List<SpendingConfigEntity> findEffectiveAsOf(@Param("userId") Long userId,
                                                 @Param("targetMonth") LocalDate targetMonth);

    @Query("SELECT s FROM SpendingConfigEntity s "
            + "WHERE s.userId = :userId AND s.effectiveFromMonth <= :endMonth "
            + "ORDER BY s.category ASC, s.effectiveFromMonth ASC")
    List<SpendingConfigEntity> findAllUpTo(@Param("userId") Long userId,
                                           @Param("endMonth") LocalDate endMonth);

    @Query("SELECT DISTINCT s.effectiveFromMonth FROM SpendingConfigEntity s "
            + "WHERE s.userId = :userId ORDER BY s.effectiveFromMonth DESC")
    List<LocalDate> findDistinctMonths(@Param("userId") Long userId);

    void deleteByUserIdAndCategoryAndEffectiveFromMonth(
            Long userId, SpendingCategory category, LocalDate effectiveFromMonth);
}