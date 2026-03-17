package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorMetadataRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.EcosIndicatorMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EcosIndicatorMetadataRepositoryImpl implements EcosIndicatorMetadataRepository {

    private final EcosIndicatorMetadataJpaRepository jpaRepository;
    private final EcosIndicatorMetadataMapper mapper;

    @Override
    public List<EcosIndicatorMetadata> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void saveAll(List<EcosIndicatorMetadata> metadataList) {
        List<EcosIndicatorMetadataEntity> entities = metadataList.stream()
            .map(mapper::toEntity)
            .toList();
        jpaRepository.saveAll(entities);
    }
}