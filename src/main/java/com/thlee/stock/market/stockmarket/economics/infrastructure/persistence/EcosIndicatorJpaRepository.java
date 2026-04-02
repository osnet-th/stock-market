package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface EcosIndicatorJpaRepository extends JpaRepository<EcosIndicatorEntity, Long> {

    boolean existsFirstBy();

    @Query(value = """
            SELECT t.id, t.class_name, t.keystat_name, t.data_value, t.cycle, t.unit_name, t.snapshot_date, t.created_at
            FROM (
                SELECT e.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY e.class_name, e.keystat_name, e.cycle
                           ORDER BY e.snapshot_date DESC
                       ) AS rn
                FROM ecos_indicator e
                WHERE e.class_name IN (:classNames)
            ) t
            WHERE t.rn = 1
            ORDER BY t.class_name, t.keystat_name, t.cycle
            """, nativeQuery = true)
    List<EcosIndicatorEntity> findLatestHistoryByClassNames(@Param("classNames") Set<String> classNames);
}
