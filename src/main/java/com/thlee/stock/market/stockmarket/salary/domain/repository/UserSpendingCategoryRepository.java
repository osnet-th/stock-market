package com.thlee.stock.market.stockmarket.salary.domain.repository;

import com.thlee.stock.market.stockmarket.salary.domain.model.UserSpendingCategory;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 정의 지출 카테고리 Repository 포트.
 */
public interface UserSpendingCategoryRepository {

    UserSpendingCategory save(UserSpendingCategory category);

    List<UserSpendingCategory> saveAll(List<UserSpendingCategory> categories);

    /** 활성/비활성 포함 전체 (sort_order ASC, id ASC) */
    List<UserSpendingCategory> findAllByUserId(Long userId);

    Optional<UserSpendingCategory> findByUserIdAndCode(Long userId, String code);

    long countByUserId(Long userId);
}
