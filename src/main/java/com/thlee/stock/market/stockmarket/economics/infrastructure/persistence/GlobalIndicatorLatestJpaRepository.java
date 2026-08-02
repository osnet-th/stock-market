package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GlobalIndicatorLatestJpaRepository
    extends JpaRepository<GlobalIndicatorLatestEntity, GlobalIndicatorLatestEntity.LatestId> {

    List<GlobalIndicatorLatestEntity> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime after);

    @Query("select max(e.lastCollectedAt) from GlobalIndicatorLatestEntity e")
    Optional<LocalDateTime> findMaxLastCollectedAt();
}