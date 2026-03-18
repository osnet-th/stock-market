package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashStockLink;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.CashStockLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CashStockLinkRepositoryImpl implements CashStockLinkRepository {

    private final CashStockLinkJpaRepository jpaRepository;

    @Override
    public CashStockLink save(CashStockLink link) {
        CashStockLinkEntity entity = toEntity(link);
        CashStockLinkEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<CashStockLink> findByStockItemId(Long stockItemId) {
        return jpaRepository.findByStockItemId(stockItemId).map(this::toDomain);
    }

    @Override
    public List<CashStockLink> findByCashItemId(Long cashItemId) {
        return jpaRepository.findByCashItemId(cashItemId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CashStockLink> findByStockItemIdIn(List<Long> stockItemIds) {
        return jpaRepository.findByStockItemIdIn(stockItemIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByStockItemId(Long stockItemId) {
        jpaRepository.deleteByStockItemId(stockItemId);
    }

    @Override
    public boolean existsByCashItemId(Long cashItemId) {
        return jpaRepository.existsByCashItemId(cashItemId);
    }

    private CashStockLinkEntity toEntity(CashStockLink link) {
        return new CashStockLinkEntity(
                link.getId(), link.getCashItemId(), link.getStockItemId(), link.getCreatedAt()
        );
    }

    private CashStockLink toDomain(CashStockLinkEntity entity) {
        return new CashStockLink(
                entity.getId(), entity.getCashItemId(), entity.getStockItemId(), entity.getCreatedAt()
        );
    }
}