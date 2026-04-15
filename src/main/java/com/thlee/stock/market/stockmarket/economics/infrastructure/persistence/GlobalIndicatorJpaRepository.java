package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GlobalIndicatorJpaRepository extends JpaRepository<GlobalIndicatorEntity, Long> {

    boolean existsFirstBy();

    @Query(value = """
            SELECT t.id, t.country_name, t.indicator_type, t.data_value, t.cycle, t.unit, t.snapshot_date, t.created_at
            FROM (
                SELECT e.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY e.country_name, e.cycle
                           ORDER BY e.snapshot_date DESC
                       ) AS rn
                FROM global_indicator e
                WHERE e.indicator_type = :indicatorType
            ) t
            WHERE t.rn = 1
            ORDER BY t.country_name, t.cycle
            """, nativeQuery = true)
    List<GlobalIndicatorEntity> findLatestHistoryByIndicatorType(@Param("indicatorType") String indicatorType);
}