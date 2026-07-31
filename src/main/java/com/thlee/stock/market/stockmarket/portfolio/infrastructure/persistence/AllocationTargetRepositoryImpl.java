package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.AllocationTarget;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.AllocationTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AllocationTargetRepositoryImpl implements AllocationTargetRepository {

    private final AllocationTargetJpaRepository targetJpaRepository;
    private final AllocationTargetAssetJpaRepository assetJpaRepository;

    @Override
    public AllocationTarget save(AllocationTarget target) {
        AllocationTargetEntity saved = targetJpaRepository.save(toEntity(target));

        // 자산군 비율은 전체 교체 (upsert 단순화)
        assetJpaRepository.deleteByAllocationTargetId(saved.getId());
        List<AllocationTargetAssetEntity> assetEntities = target.getInvestAssetRatios().entrySet().stream()
                .map(entry -> new AllocationTargetAssetEntity(
                        null, saved.getId(), entry.getKey().name(), entry.getValue()))
                .toList();
        List<AllocationTargetAssetEntity> savedAssets = assetJpaRepository.saveAll(assetEntities);

        return toDomain(saved, savedAssets);
    }

    @Override
    public Optional<AllocationTarget> findByUserId(Long userId) {
        return targetJpaRepository.findByUserId(userId)
                .map(entity -> toDomain(entity,
                        assetJpaRepository.findByAllocationTargetIdOrderByIdAsc(entity.getId())));
    }

    private AllocationTargetEntity toEntity(AllocationTarget target) {
        return new AllocationTargetEntity(
                target.getId(),
                target.getUserId(),
                target.getSafeRatio(),
                target.getInvestRatio(),
                target.getBandPctPoint(),
                target.getCreatedAt(),
                target.getUpdatedAt()
        );
    }

    private AllocationTarget toDomain(AllocationTargetEntity entity,
                                      List<AllocationTargetAssetEntity> assetEntities) {
        Map<AssetType, BigDecimal> ratios = new LinkedHashMap<>();
        for (AllocationTargetAssetEntity asset : assetEntities) {
            ratios.put(AssetType.valueOf(asset.getAssetType()), asset.getTargetRatio());
        }
        return new AllocationTarget(
                entity.getId(),
                entity.getUserId(),
                entity.getSafeRatio(),
                entity.getInvestRatio(),
                entity.getBandPctPoint(),
                ratios,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
