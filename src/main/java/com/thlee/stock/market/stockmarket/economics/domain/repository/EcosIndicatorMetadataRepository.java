package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;

import java.util.List;

public interface EcosIndicatorMetadataRepository {

    List<EcosIndicatorMetadata> findAll();

    long count();

    void saveAll(List<EcosIndicatorMetadata> metadataList);
}