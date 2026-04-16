package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.EcosIndicatorLatestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EcosIndicatorLatestRepositoryImpl implements EcosIndicatorLatestRepository {

    private final EcosIndicatorLatestJpaRepository jpaRepository;
    private final EcosIndicatorLatestMapper mapper;

    @Override
    public List<EcosIndicatorLatest> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<EcosIndicatorLatest> findByClassNameAndKeystatName(String className, String keystatName) {
        EcosIndicatorLatestEntity.LatestId id = new EcosIndicatorLatestEntity.LatestId(className, keystatName);
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<EcosIndicatorLatest> findRecentlyUpdated(LocalDateTime after) {
        return jpaRepository.findByUpdatedAtAfterOrderByUpdatedAtDesc(after).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void saveAll(List<EcosIndicatorLatest> latestList) {
        for (EcosIndicatorLatest latest : latestList) {
            EcosIndicatorLatestEntity.LatestId id =
                new EcosIndicatorLatestEntity.LatestId(latest.getClassName(), latest.getKeystatName());

            Optional<EcosIndicatorLatestEntity> existing = jpaRepository.findById(id);

            if (existing.isPresent()) {
                EcosIndicatorLatestEntity entity = existing.get();
                boolean cycleChanged = latest.getCycle() != null
                    && !latest.getCycle().equals(entity.getCycle());
                LocalDateTime updatedAt = cycleChanged
                    ? LocalDateTime.now()
                    : entity.getUpdatedAt();
                entity.update(latest.getDataValue(), latest.getCycle(), cycleChanged, updatedAt);
            } else {
                jpaRepository.save(mapper.toEntity(latest));
            }
        }
    }
}
