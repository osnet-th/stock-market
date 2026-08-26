package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RealEstateMarketLatestJpaRepository
        extends JpaRepository<RealEstateMarketLatestEntity, RealEstateMarketLatestEntity.LatestId> {

    List<RealEstateMarketLatestEntity> findAllByRegionCodeAndCategoryAndSource(
            String regionCode,
            RealEstateMarketCategory category,
            RealEstateMarketSource source);

    Optional<RealEstateMarketLatestEntity> findByRegionCodeAndCategoryAndSourceAndIndicatorCode(
            String regionCode,
            RealEstateMarketCategory category,
            RealEstateMarketSource source,
            String indicatorCode);
}
