package com.thlee.stock.market.stockmarket.salary.domain.repository;

import com.thlee.stock.market.stockmarket.salary.domain.model.SalarySetting;

import java.util.Optional;

/**
 * 월급 화면 사용자 설정 Repository 포트.
 */
public interface SalarySettingRepository {

    SalarySetting save(SalarySetting setting);

    Optional<SalarySetting> findByUserId(Long userId);
}
