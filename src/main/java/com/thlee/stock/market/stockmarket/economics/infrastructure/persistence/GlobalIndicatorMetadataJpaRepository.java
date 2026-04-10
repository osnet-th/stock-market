package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalIndicatorMetadataJpaRepository
    extends JpaRepository<GlobalIndicatorMetadataEntity, GlobalEconomicIndicatorType> {
}