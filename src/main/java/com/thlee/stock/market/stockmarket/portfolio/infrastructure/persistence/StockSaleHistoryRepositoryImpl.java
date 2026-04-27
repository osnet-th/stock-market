package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockSaleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StockSaleHistoryRepositoryImpl implements StockSaleHistoryRepository {

    private final StockSaleHistoryJpaRepository jpaRepository;

    @Override
    public StockSaleHistory save(StockSaleHistory history) {
        StockSaleHistoryEntity entity = toEntity(history);
        StockSaleHistoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<StockSaleHistory> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<StockSaleHistory> findByPortfolioItemId(Long portfolioItemId) {
        return jpaRepository.findByPortfolioItemIdOrderBySoldAtAsc(portfolioItemId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockSaleHistory> findByPortfolioItemIdIn(List<Long> portfolioItemIds) {
        if (portfolioItemIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByPortfolioItemIdInOrderBySoldAtAsc(portfolioItemIds)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockSaleHistory> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(StockSaleHistory history) {
        jpaRepository.deleteById(history.getId());
    }

    @Override
    public void deleteByPortfolioItemId(Long portfolioItemId) {
        jpaRepository.deleteByPortfolioItemId(portfolioItemId);
    }

    private StockSaleHistoryEntity toEntity(StockSaleHistory h) {
        return new StockSaleHistoryEntity(
                h.getId(), h.getPortfolioItemId(), h.getQuantity(),
                h.getAvgBuyPrice(), h.getSalePrice(),
                h.getProfit(), h.getProfitRate(), h.getContributionRate(),
                h.getTotalAssetAtSale(), h.getCurrency(), h.getFxRate(),
                h.getSalePriceKrw(), h.getProfitKrw(),
                h.getReason(), h.getMemo(), h.getStockCode(), h.getStockName(),
                h.isUnrecordedDeposit(), h.getSoldAt(),
                h.getCreatedAt(), h.getUpdatedAt()
        );
    }

    private StockSaleHistory toDomain(StockSaleHistoryEntity e) {
        return new StockSaleHistory(
                e.getId(), e.getPortfolioItemId(), e.getQuantity(),
                e.getAvgBuyPrice(), e.getSalePrice(),
                e.getProfit(), e.getProfitRate(), e.getContributionRate(),
                e.getTotalAssetAtSale(), e.getCurrency(), e.getFxRate(),
                e.getSalePriceKrw(), e.getProfitKrw(),
                e.getReason(), e.getMemo(), e.getStockCode(), e.getStockName(),
                e.isUnrecordedDeposit(), e.getSoldAt(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
