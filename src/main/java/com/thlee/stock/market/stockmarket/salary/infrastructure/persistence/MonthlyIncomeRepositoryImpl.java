package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.repository.MonthlyIncomeRepository;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper.MonthlyIncomeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MonthlyIncomeRepositoryImpl implements MonthlyIncomeRepository {

    private final MonthlyIncomeJpaRepository jpaRepository;

    @Override
    public MonthlyIncome save(MonthlyIncome income) {
        MonthlyIncomeEntity saved = jpaRepository.save(MonthlyIncomeMapper.toEntity(income));
        return MonthlyIncomeMapper.toDomain(saved);
    }

    @Override
    public Optional<MonthlyIncome> findByUserIdAndEffectiveFromMonth(Long userId, YearMonth yearMonth) {
        return jpaRepository
                .findByUserIdAndEffectiveFromMonth(userId, MonthlyIncomeMapper.toLocalDate(yearMonth))
                .map(MonthlyIncomeMapper::toDomain);
    }

    @Override
    public Optional<MonthlyIncome> findEffectiveAsOf(Long userId, YearMonth targetMonth) {
        return jpaRepository
                .findEffectiveAsOf(userId, MonthlyIncomeMapper.toLocalDate(targetMonth))
                .map(MonthlyIncomeMapper::toDomain);
    }

    @Override
    public List<MonthlyIncome> findAllUpTo(Long userId, YearMonth endMonth) {
        return jpaRepository
                .findAllUpTo(userId, MonthlyIncomeMapper.toLocalDate(endMonth)).stream()
                .map(MonthlyIncomeMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<YearMonth> findDistinctMonths(Long userId) {
        return jpaRepository.findDistinctMonths(userId).stream()
                .map(MonthlyIncomeMapper::toYearMonth)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUserIdAndEffectiveFromMonth(Long userId, YearMonth yearMonth) {
        jpaRepository.deleteByUserIdAndEffectiveFromMonth(userId, MonthlyIncomeMapper.toLocalDate(yearMonth));
    }
}