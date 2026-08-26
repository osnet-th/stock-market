package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketIndicatorRepository;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper.RealEstateMarketIndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RealEstateMarketIndicatorRepositoryImpl implements RealEstateMarketIndicatorRepository {

    private final RealEstateMarketIndicatorJpaRepository jpaRepository;
    private final RealEstateMarketIndicatorMapper mapper;

    @Override
    public List<RealEstateMarketIndicator> saveAll(List<RealEstateMarketIndicator> indicators) {
        List<RealEstateMarketIndicatorEntity> entities = indicators.stream()
                .map(mapper::toEntity)
                .toList();
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RealEstateMarketIndicator> findHistory(String regionCode,
                                                       RealEstateMarketCategory category,
                                                       RealEstateMarketSource source,
                                                       String indicatorCode,
                                                       int limit) {
        return jpaRepository.findHistoryByEnums(regionCode, category, source, indicatorCode, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RealEstateMarketIndicator> findLatestByRegionAndCategory(String regionCode,
                                                                         RealEstateMarketCategory category) {
        return jpaRepository.findLatestPerSourceByEnums(regionCode, category).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
