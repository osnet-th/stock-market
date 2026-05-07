package com.thlee.stock.market.stockmarket.realestate.domain.repository;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketMetadata;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;

import java.util.List;
import java.util.Optional;

public interface RealEstateMarketMetadataRepository {

    List<RealEstateMarketMetadata> findAll();

    Optional<RealEstateMarketMetadata> findByKey(RealEstateMarketCategory category,
                                                  RealEstateMarketSource source,
                                                  String indicatorCode);

    void saveAll(List<RealEstateMarketMetadata> metadataList);

    long count();
}
