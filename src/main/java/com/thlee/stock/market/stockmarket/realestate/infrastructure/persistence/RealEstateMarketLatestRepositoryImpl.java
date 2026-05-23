package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketLatest;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketLatestRepository;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper.RealEstateMarketLatestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RealEstateMarketLatestRepositoryImpl implements RealEstateMarketLatestRepository {

    private final RealEstateMarketLatestJpaRepository jpaRepository;
    private final RealEstateMarketLatestMapper mapper;

    @Override
    public List<RealEstateMarketLatest> findAllByRegionAndCategoryAndSource(String regionCode,
                                                                            RealEstateMarketCategory category,
                                                                            RealEstateMarketSource source) {
        return jpaRepository.findAllByRegionCodeAndCategoryAndSource(regionCode, category, source).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RealEstateMarketLatest> findByKey(String regionCode,
                                                      RealEstateMarketCategory category,
                                                      RealEstateMarketSource source,
                                                      String indicatorCode) {
        return jpaRepository
                .findByRegionCodeAndCategoryAndSourceAndIndicatorCode(regionCode, category, source, indicatorCode)
                .map(mapper::toDomain);
    }

    @Override
    public void upsertAll(List<RealEstateMarketLatest> latests) {
        List<RealEstateMarketLatestEntity> entities = latests.stream()
                .map(mapper::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<RealEstateMarketLatest> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
