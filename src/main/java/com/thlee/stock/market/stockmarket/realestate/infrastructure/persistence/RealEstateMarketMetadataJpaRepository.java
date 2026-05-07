package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RealEstateMarketMetadataJpaRepository
        extends JpaRepository<RealEstateMarketMetadataEntity, RealEstateMarketMetadataEntity.MetadataId> {

    Optional<RealEstateMarketMetadataEntity> findByCategoryAndSourceAndIndicatorCode(
            RealEstateMarketCategory category,
            RealEstateMarketSource source,
            String indicatorCode);
}
