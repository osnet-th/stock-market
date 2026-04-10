package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorMetadataRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.GlobalIndicatorMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GlobalIndicatorMetadataRepositoryImpl implements GlobalIndicatorMetadataRepository {

    private final GlobalIndicatorMetadataJpaRepository jpaRepository;
    private final GlobalIndicatorMetadataMapper mapper;

    @Override
    public List<GlobalIndicatorMetadata> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void saveAll(List<GlobalIndicatorMetadata> metadataList) {
        List<GlobalIndicatorMetadataEntity> entities = metadataList.stream()
            .map(mapper::toEntity)
            .toList();
        jpaRepository.saveAll(entities);
    }
}