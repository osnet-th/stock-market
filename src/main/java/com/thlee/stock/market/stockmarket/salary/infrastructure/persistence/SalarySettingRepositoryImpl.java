package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.SalarySetting;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SalarySettingRepository;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper.SalarySettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SalarySettingRepositoryImpl implements SalarySettingRepository {

    private final SalarySettingJpaRepository jpaRepository;

    @Override
    public SalarySetting save(SalarySetting setting) {
        SalarySettingEntity saved = jpaRepository.save(SalarySettingMapper.toEntity(setting));
        return SalarySettingMapper.toDomain(saved);
    }

    @Override
    public Optional<SalarySetting> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(SalarySettingMapper::toDomain);
    }
}
