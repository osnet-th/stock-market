package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotEntity, Long> {

    Optional<PortfolioSnapshotEntity> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    List<PortfolioSnapshotEntity> findByUserIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
            Long userId, LocalDate fromDate);
}
