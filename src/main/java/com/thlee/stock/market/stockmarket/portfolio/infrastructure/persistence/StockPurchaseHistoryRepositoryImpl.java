package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StockPurchaseHistoryRepositoryImpl implements StockPurchaseHistoryRepository {

    private final StockPurchaseHistoryJpaRepository jpaRepository;

    @Override
    public StockPurchaseHistory save(StockPurchaseHistory history) {
        StockPurchaseHistoryEntity entity = toEntity(history);
        StockPurchaseHistoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<StockPurchaseHistory> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<StockPurchaseHistory> findByPortfolioItemId(Long portfolioItemId) {
        return jpaRepository.findByPortfolioItemIdOrderByPurchasedAtAsc(portfolioItemId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(StockPurchaseHistory history) {
        jpaRepository.deleteById(history.getId());
    }

    @Override
    public void deleteByPortfolioItemId(Long portfolioItemId) {
        jpaRepository.deleteByPortfolioItemId(portfolioItemId);
    }

    private StockPurchaseHistoryEntity toEntity(StockPurchaseHistory h) {
        return new StockPurchaseHistoryEntity(
                h.getId(), h.getPortfolioItemId(), h.getQuantity(),
                h.getPurchasePrice(), h.getPurchasedAt(), h.getMemo(), h.getCreatedAt()
        );
    }

    private StockPurchaseHistory toDomain(StockPurchaseHistoryEntity e) {
        return new StockPurchaseHistory(
                e.getId(), e.getPortfolioItemId(), e.getQuantity(),
                e.getPurchasePrice(), e.getPurchasedAt(), e.getMemo(), e.getCreatedAt()
        );
    }
}