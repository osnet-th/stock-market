package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PortfolioItem JPA Repository
 */
public interface PortfolioItemJpaRepository extends JpaRepository<PortfolioItemEntity, Long> {

    List<PortfolioItemEntity> findByUserIdAndStatus(Long userId, PortfolioItemStatus status);

    List<PortfolioItemEntity> findByNewsEnabledAndStatus(boolean newsEnabled, PortfolioItemStatus status);

    boolean existsByUserIdAndItemNameAndAssetTypeAndStatus(Long userId,
                                                           String itemName,
                                                           String assetType,
                                                           PortfolioItemStatus status);

    List<PortfolioItemEntity> findByUserIdAndItemNameAndNewsEnabledAndStatus(Long userId,
                                                                            String itemName,
                                                                            boolean newsEnabled,
                                                                            PortfolioItemStatus status);

    List<PortfolioItemEntity> findByUserIdInAndStatus(List<Long> userIds, PortfolioItemStatus status);

    @Query("select distinct i.userId from PortfolioItemEntity i where i.status = :status")
    List<Long> findDistinctUserIdsByStatus(@Param("status") PortfolioItemStatus status);
}