package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository {

    PortfolioSnapshot save(PortfolioSnapshot snapshot);

    Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    /**
     * 기준일 이후 스냅샷을 날짜 오름차순으로 조회 (자산 추이 차트용)
     */
    List<PortfolioSnapshot> findByUserIdSince(Long userId, LocalDate fromDate);
}
