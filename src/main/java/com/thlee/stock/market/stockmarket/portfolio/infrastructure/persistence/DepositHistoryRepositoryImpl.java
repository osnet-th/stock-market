package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.DepositHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.DepositHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DepositHistoryRepositoryImpl implements DepositHistoryRepository {

    private final DepositHistoryJpaRepository jpaRepository;

    @Override
    public DepositHistory save(DepositHistory history) {
        DepositHistoryEntity entity = toEntity(history);
        DepositHistoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DepositHistory> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DepositHistory> findByPortfolioItemId(Long portfolioItemId) {
        return jpaRepository.findByPortfolioItemIdOrderByDepositDateAsc(portfolioItemId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DepositHistory> findByPortfolioItemIdIn(List<Long> portfolioItemIds) {
        return jpaRepository.findByPortfolioItemIdInOrderByDepositDateAsc(portfolioItemIds)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(DepositHistory history) {
        jpaRepository.deleteById(history.getId());
    }

    @Override
    public void deleteByPortfolioItemId(Long portfolioItemId) {
        jpaRepository.deleteByPortfolioItemId(portfolioItemId);
    }

    private DepositHistoryEntity toEntity(DepositHistory h) {
        return new DepositHistoryEntity(
                h.getId(), h.getPortfolioItemId(), h.getDepositDate(),
                h.getAmount(), h.getUnits(), h.getMemo(), h.getCreatedAt()
        );
    }

    private DepositHistory toDomain(DepositHistoryEntity e) {
        return new DepositHistory(
                e.getId(), e.getPortfolioItemId(), e.getDepositDate(),
                e.getAmount(), e.getUnits(), e.getMemo(), e.getCreatedAt()
        );
    }
}