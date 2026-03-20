package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * PortfolioItem JPA Repository
 */
public interface PortfolioItemJpaRepository extends JpaRepository<PortfolioItemEntity, Long> {

    List<PortfolioItemEntity> findByUserId(Long userId);

    List<PortfolioItemEntity> findByNewsEnabled(boolean newsEnabled);

    boolean existsByUserIdAndItemNameAndAssetType(Long userId, String itemName, String assetType);

    List<PortfolioItemEntity> findByUserIdAndItemNameAndNewsEnabled(Long userId, String itemName, boolean newsEnabled);
}