package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSpendingCategoryJpaRepository extends JpaRepository<UserSpendingCategoryEntity, Long> {

    List<UserSpendingCategoryEntity> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    Optional<UserSpendingCategoryEntity> findByUserIdAndCode(Long userId, String code);

    long countByUserId(Long userId);
}
