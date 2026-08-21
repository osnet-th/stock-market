package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItemSet;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SpendingItemSetRepository;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper.SpendingItemSetMapper;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper.YearMonthConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SpendingItemSetRepositoryImpl implements SpendingItemSetRepository {

    private final SpendingItemSetJpaRepository setJpaRepository;
    private final SpendingItemJpaRepository itemJpaRepository;

    /** 헤더 upsert 후 항목 행을 전체 교체한다 (스냅샷 저장). */
    @Override
    public SpendingItemSet save(SpendingItemSet itemSet) {
        SpendingItemSetEntity header = setJpaRepository.save(SpendingItemSetMapper.toHeaderEntity(itemSet));
        itemJpaRepository.deleteBySetId(header.getId());
        List<SpendingItemEntity> items = itemJpaRepository.saveAll(toItemEntities(itemSet, header.getId()));
        return SpendingItemSetMapper.toDomain(header, items);
    }

    @Override
    public Optional<SpendingItemSet> findByUserIdAndEffectiveFromMonth(Long userId, YearMonth yearMonth) {
        return setJpaRepository
                .findByUserIdAndEffectiveFromMonth(userId, YearMonthConverter.toLocalDate(yearMonth))
                .map(this::toDomainWithItems);
    }

    @Override
    public Optional<SpendingItemSet> findEffectiveAsOf(Long userId, YearMonth targetMonth) {
        return setJpaRepository
                .findFirstByUserIdAndEffectiveFromMonthLessThanEqualOrderByEffectiveFromMonthDesc(
                        userId, YearMonthConverter.toLocalDate(targetMonth))
                .map(this::toDomainWithItems);
    }

    private SpendingItemSet toDomainWithItems(SpendingItemSetEntity header) {
        return SpendingItemSetMapper.toDomain(
                header, itemJpaRepository.findBySetIdOrderBySortOrderAsc(header.getId()));
    }

    private static List<SpendingItemEntity> toItemEntities(SpendingItemSet itemSet, Long setId) {
        LocalDateTime now = LocalDateTime.now();
        return itemSet.getItems().stream()
                .map(item -> SpendingItemSetMapper.toItemEntity(item, setId, now))
                .collect(Collectors.toList());
    }
}
