package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyIncomeJpaRepository extends JpaRepository<MonthlyIncomeEntity, Long> {

    Optional<MonthlyIncomeEntity> findByUserIdAndEffectiveFromMonth(Long userId, LocalDate effectiveFromMonth);

    /**
     * 해당 월 시점의 유효 월급(상속 포함). PostgreSQL 전용(LIMIT 사용).
     */
    @Query(value = """
            SELECT m.*
            FROM monthly_income m
            WHERE m.user_id = :userId
              AND m.effective_from_month <= :targetMonth
            ORDER BY m.effective_from_month DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<MonthlyIncomeEntity> findEffectiveAsOf(@Param("userId") Long userId,
                                                    @Param("targetMonth") LocalDate targetMonth);

    @Query("SELECT m FROM MonthlyIncomeEntity m "
            + "WHERE m.userId = :userId AND m.effectiveFromMonth <= :endMonth "
            + "ORDER BY m.effectiveFromMonth ASC")
    List<MonthlyIncomeEntity> findAllUpTo(@Param("userId") Long userId,
                                          @Param("endMonth") LocalDate endMonth);

    @Query("SELECT DISTINCT m.effectiveFromMonth FROM MonthlyIncomeEntity m "
            + "WHERE m.userId = :userId ORDER BY m.effectiveFromMonth DESC")
    List<LocalDate> findDistinctMonths(@Param("userId") Long userId);

    void deleteByUserIdAndEffectiveFromMonth(Long userId, LocalDate effectiveFromMonth);
}