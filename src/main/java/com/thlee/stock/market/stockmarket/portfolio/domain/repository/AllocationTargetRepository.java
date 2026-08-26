package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.AllocationTarget;

import java.util.Optional;

public interface AllocationTargetRepository {

    AllocationTarget save(AllocationTarget target);

    Optional<AllocationTarget> findByUserId(Long userId);
}
