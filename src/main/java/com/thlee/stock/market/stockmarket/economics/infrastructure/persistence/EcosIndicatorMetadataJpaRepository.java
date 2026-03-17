package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EcosIndicatorMetadataJpaRepository
    extends JpaRepository<EcosIndicatorMetadataEntity, EcosIndicatorMetadataEntity.MetadataId> {
}