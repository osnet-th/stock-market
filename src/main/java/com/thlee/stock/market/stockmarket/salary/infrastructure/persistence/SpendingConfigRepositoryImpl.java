package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SpendingConfigRepository;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper.SpendingConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SpendingConfigRepositoryImpl implements SpendingConfigRepository {

    private final SpendingConfigJpaRepository jpaRepository;

    @Override
    public SpendingConfig save(SpendingConfig config) {
        SpendingConfigEntity saved = jpaRepository.save(SpendingConfigMapper.toEntity(config));
        return SpendingConfigMapper.toDomain(saved);
    }

    @Override
    public Optional<SpendingConfig> findByUserIdAndCategoryAndEffectiveFromMonth(
            Long userId, SpendingCategory category, YearMonth yearMonth) {
        return jpaRepository
                .findByUserIdAndCategoryAndEffectiveFromMonth(
                        userId, category, SpendingConfigMapper.toLocalDate(yearMonth))
                .map(SpendingConfigMapper::toDomain);
    }

    @Override
    public List<SpendingConfig> findEffectiveAsOf(Long userId, YearMonth targetMonth) {
        return jpaRepository
                .findEffectiveAsOf(userId, SpendingConfigMapper.toLocalDate(targetMonth)).stream()
                .map(SpendingConfigMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpendingConfig> findAllUpTo(Long userId, YearMonth endMonth) {
        return jpaRepository
                .findAllUpTo(userId, SpendingConfigMapper.toLocalDate(endMonth)).stream()
                .map(SpendingConfigMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<YearMonth> findDistinctMonths(Long userId) {
        return jpaRepository.findDistinctMonths(userId).stream()
                .map(SpendingConfigMapper::toYearMonth)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUserIdAndCategoryAndEffectiveFromMonth(
            Long userId, SpendingCategory category, YearMonth yearMonth) {
        jpaRepository.deleteByUserIdAndCategoryAndEffectiveFromMonth(
                userId, category, SpendingConfigMapper.toLocalDate(yearMonth));
    }
}