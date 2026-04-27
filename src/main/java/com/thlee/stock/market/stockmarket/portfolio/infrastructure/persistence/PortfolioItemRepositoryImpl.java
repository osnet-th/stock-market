package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence.mapper.PortfolioItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PortfolioItemRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class PortfolioItemRepositoryImpl implements PortfolioItemRepository {

    private final PortfolioItemJpaRepository portfolioItemJpaRepository;

    @Override
    public PortfolioItem save(PortfolioItem item) {
        PortfolioItemEntity entity = PortfolioItemMapper.toEntity(item);
        PortfolioItemEntity savedEntity = portfolioItemJpaRepository.save(entity);
        return PortfolioItemMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PortfolioItem> findById(Long id) {
        return portfolioItemJpaRepository.findById(id)
                .map(PortfolioItemMapper::toDomain);
    }

    @Override
    public List<PortfolioItem> findByUserId(Long userId) {
        return portfolioItemJpaRepository.findByUserIdAndStatus(userId, PortfolioItemStatus.ACTIVE)
                .stream()
                .map(PortfolioItemMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioItem> findByUserIdIncludingClosed(Long userId) {
        return portfolioItemJpaRepository.findByUserId(userId)
                .stream()
                .map(PortfolioItemMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioItem> findByNewsEnabled(boolean newsEnabled) {
        return portfolioItemJpaRepository.findByNewsEnabledAndStatus(newsEnabled, PortfolioItemStatus.ACTIVE)
                .stream()
                .map(PortfolioItemMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioItem> findByUserIdAndItemNameAndNewsEnabled(Long userId, String itemName, boolean newsEnabled) {
        return portfolioItemJpaRepository.findByUserIdAndItemNameAndNewsEnabledAndStatus(
                        userId, itemName, newsEnabled, PortfolioItemStatus.ACTIVE)
                .stream()
                .map(PortfolioItemMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndItemNameAndAssetType(Long userId, String itemName, AssetType assetType) {
        return portfolioItemJpaRepository.existsByUserIdAndItemNameAndAssetTypeAndStatus(
                userId, itemName, assetType.name(), PortfolioItemStatus.ACTIVE);
    }

    @Override
    public List<PortfolioItem> findByUserIdIn(List<Long> userIds) {
        return portfolioItemJpaRepository.findByUserIdInAndStatus(userIds, PortfolioItemStatus.ACTIVE)
                .stream()
                .map(PortfolioItemMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(PortfolioItem item) {
        PortfolioItemEntity entity = PortfolioItemMapper.toEntity(item);
        portfolioItemJpaRepository.delete(entity);
    }
}