package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocationTargetAssetJpaRepository extends JpaRepository<AllocationTargetAssetEntity, Long> {

    List<AllocationTargetAssetEntity> findByAllocationTargetIdOrderByIdAsc(Long allocationTargetId);

    void deleteByAllocationTargetId(Long allocationTargetId);
}
