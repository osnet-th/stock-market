package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.EcosIndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class EcosIndicatorRepositoryImpl implements EcosIndicatorRepository {

    private final EcosIndicatorJpaRepository jpaRepository;
    private final EcosIndicatorMapper mapper;

    @Override
    public List<EcosIndicator> saveAll(List<EcosIndicator> indicators) {
        List<EcosIndicatorEntity> entities = indicators.stream()
            .map(mapper::toEntity)
            .toList();

        List<EcosIndicatorEntity> saved = jpaRepository.saveAll(entities);

        return saved.stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsAny() {
        return jpaRepository.existsFirstBy();
    }

    @Override
    public List<EcosIndicator> findLatestHistoryByClassNames(Set<String> classNames) {
        return jpaRepository.findLatestHistoryByClassNames(classNames).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
