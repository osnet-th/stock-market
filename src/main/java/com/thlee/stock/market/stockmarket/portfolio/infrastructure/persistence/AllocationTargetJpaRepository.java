package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllocationTargetJpaRepository extends JpaRepository<AllocationTargetEntity, Long> {

    Optional<AllocationTargetEntity> findByUserId(Long userId);
}
