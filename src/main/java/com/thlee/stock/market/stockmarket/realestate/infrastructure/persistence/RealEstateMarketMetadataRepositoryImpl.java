package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketMetadata;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketMetadataRepository;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper.RealEstateMarketMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RealEstateMarketMetadataRepositoryImpl implements RealEstateMarketMetadataRepository {

    private final RealEstateMarketMetadataJpaRepository jpaRepository;
    private final RealEstateMarketMetadataMapper mapper;

    @Override
    public List<RealEstateMarketMetadata> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RealEstateMarketMetadata> findByKey(RealEstateMarketCategory category,
                                                        RealEstateMarketSource source,
                                                        String indicatorCode) {
        return jpaRepository
                .findByCategoryAndSourceAndIndicatorCode(category, source, indicatorCode)
                .map(mapper::toDomain);
    }

    @Override
    public void saveAll(List<RealEstateMarketMetadata> metadataList) {
        List<RealEstateMarketMetadataEntity> entities = metadataList.stream()
                .map(mapper::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
