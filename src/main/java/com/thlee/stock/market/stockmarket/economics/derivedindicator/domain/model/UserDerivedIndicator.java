package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 커스텀 파생지표 도메인 모델.
 * <p>
 * userId 기반 소유. 수식은 구조형 DerivedFormula. category는 사용자 정의의 경우 단일 카테고리,
 * 교차 카테고리(시스템 프리셋 복제)인 경우 null(혼합).
 */
@Getter
public class UserDerivedIndicator {

    private final Long id;
    private final Long userId;
    private final String name;
    private final String unit;
    private final DerivedFormula formula;
    private final EcosIndicatorCategory category;
    private final LocalDateTime createdAt;

    public UserDerivedIndicator(Long id,
                                Long userId,
                                String name,
                                String unit,
                                DerivedFormula formula,
                                EcosIndicatorCategory category,
                                LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.unit = unit;
        this.formula = formula;
        this.category = category;
        this.createdAt = createdAt;
    }

    /**
     * 신규 파생지표 생성. id는 영속 시 부여.
     */
    public static UserDerivedIndicator create(Long userId,
                                              String name,
                                              String unit,
                                              DerivedFormula formula,
                                              EcosIndicatorCategory category) {
        return new UserDerivedIndicator(null, userId, name, unit, formula, category, LocalDateTime.now());
    }

    /**
     * 수정. 소유권/식별자는 유지하고 표시·수식만 교체.
     */
    public UserDerivedIndicator update(String newName,
                                       String newUnit,
                                       DerivedFormula newFormula,
                                       EcosIndicatorCategory newCategory) {
        return new UserDerivedIndicator(id, userId, newName, newUnit, newFormula, newCategory, createdAt);
    }
}
