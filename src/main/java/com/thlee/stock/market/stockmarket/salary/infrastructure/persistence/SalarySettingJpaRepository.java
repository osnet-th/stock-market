package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalarySettingJpaRepository extends JpaRepository<SalarySettingEntity, Long> {

    Optional<SalarySettingEntity> findByUserId(Long userId);
}
