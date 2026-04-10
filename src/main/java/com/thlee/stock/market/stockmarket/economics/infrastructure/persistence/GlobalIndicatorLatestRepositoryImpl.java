package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.GlobalIndicatorLatestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GlobalIndicatorLatestRepositoryImpl implements GlobalIndicatorLatestRepository {

    private final GlobalIndicatorLatestJpaRepository jpaRepository;
    private final GlobalIndicatorLatestMapper mapper;

    @Override
    public List<GlobalIndicatorLatest> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void saveAll(List<GlobalIndicatorLatest> latestList) {
        for (GlobalIndicatorLatest latest : latestList) {
            GlobalIndicatorLatestEntity.LatestId id =
                new GlobalIndicatorLatestEntity.LatestId(latest.getCountryName(), latest.getIndicatorType());

            Optional<GlobalIndicatorLatestEntity> existing = jpaRepository.findById(id);

            if (existing.isPresent()) {
                GlobalIndicatorLatestEntity entity = existing.get();
                boolean cycleChanged = latest.getCycle() != null
                    && !latest.getCycle().equals(entity.getCycle());
                entity.update(
                    latest.getDataValue(),
                    latest.getCycle(),
                    latest.getUnit(),
                    cycleChanged,
                    LocalDateTime.now()
                );
            } else {
                jpaRepository.save(mapper.toEntity(latest));
            }
        }
    }
}