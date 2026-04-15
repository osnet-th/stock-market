package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.GlobalIndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GlobalIndicatorRepositoryImpl implements GlobalIndicatorRepository {

    private final GlobalIndicatorJpaRepository jpaRepository;
    private final GlobalIndicatorMapper mapper;

    @Override
    public List<GlobalIndicator> saveAll(List<GlobalIndicator> indicators) {
        List<GlobalIndicatorEntity> entities = indicators.stream()
            .map(mapper::toEntity)
            .toList();

        List<GlobalIndicatorEntity> saved = jpaRepository.saveAll(entities);

        return saved.stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsAny() {
        return jpaRepository.existsFirstBy();
    }

    @Override
    public List<GlobalIndicator> findLatestHistoryByIndicatorType(GlobalEconomicIndicatorType indicatorType) {
        return jpaRepository.findLatestHistoryByIndicatorType(indicatorType.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}