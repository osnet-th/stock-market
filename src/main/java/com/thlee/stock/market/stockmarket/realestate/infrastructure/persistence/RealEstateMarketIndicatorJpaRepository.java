package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RealEstateMarketIndicatorJpaRepository
        extends JpaRepository<RealEstateMarketIndicatorEntity, Long> {

    @Query(value = """
            SELECT t.id, t.region_code, t.category, t.source, t.indicator_code,
                   t.reference_text, t.value, t.payload, t.source_url, t.cycle,
                   t.snapshot_date, t.created_at
            FROM (
                SELECT e.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY e.region_code, e.category, e.source, e.indicator_code
                           ORDER BY e.snapshot_date DESC
                       ) AS rn
                FROM real_estate_market_indicator e
                WHERE e.region_code = :regionCode
                  AND e.category = :category
                  AND e.source = :source
                  AND e.indicator_code = :indicatorCode
            ) t
            WHERE t.rn <= :limit
            ORDER BY t.snapshot_date ASC
            """, nativeQuery = true)
    List<RealEstateMarketIndicatorEntity> findHistory(@Param("regionCode") String regionCode,
                                                      @Param("category") String category,
                                                      @Param("source") String source,
                                                      @Param("indicatorCode") String indicatorCode,
                                                      @Param("limit") int limit);

    @Query(value = """
            SELECT t.id, t.region_code, t.category, t.source, t.indicator_code,
                   t.reference_text, t.value, t.payload, t.source_url, t.cycle,
                   t.snapshot_date, t.created_at
            FROM (
                SELECT e.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY e.region_code, e.category, e.source, e.indicator_code
                           ORDER BY e.snapshot_date DESC
                       ) AS rn
                FROM real_estate_market_indicator e
                WHERE e.region_code = :regionCode
                  AND e.category = :category
            ) t
            WHERE t.rn = 1
            ORDER BY t.source, t.indicator_code
            """, nativeQuery = true)
    List<RealEstateMarketIndicatorEntity> findLatestPerSource(@Param("regionCode") String regionCode,
                                                              @Param("category") String category);

    default List<RealEstateMarketIndicatorEntity> findHistoryByEnums(String regionCode,
                                                                     RealEstateMarketCategory category,
                                                                     RealEstateMarketSource source,
                                                                     String indicatorCode,
                                                                     int limit) {
        return findHistory(regionCode, category.name(), source.name(), indicatorCode, limit);
    }

    default List<RealEstateMarketIndicatorEntity> findLatestPerSourceByEnums(String regionCode,
                                                                              RealEstateMarketCategory category) {
        return findLatestPerSource(regionCode, category.name());
    }
}
