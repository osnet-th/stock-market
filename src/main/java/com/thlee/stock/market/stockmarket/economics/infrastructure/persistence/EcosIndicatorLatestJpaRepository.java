package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EcosIndicatorLatestJpaRepository
    extends JpaRepository<EcosIndicatorLatestEntity, EcosIndicatorLatestEntity.LatestId> {

    List<EcosIndicatorLatestEntity> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime after);
}
