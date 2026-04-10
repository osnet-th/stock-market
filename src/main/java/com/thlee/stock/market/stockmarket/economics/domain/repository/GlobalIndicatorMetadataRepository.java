package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorMetadata;

import java.util.List;

public interface GlobalIndicatorMetadataRepository {

    List<GlobalIndicatorMetadata> findAll();

    long count();

    void saveAll(List<GlobalIndicatorMetadata> metadataList);
}