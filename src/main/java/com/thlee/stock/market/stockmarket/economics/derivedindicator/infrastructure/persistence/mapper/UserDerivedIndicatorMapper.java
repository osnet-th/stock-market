package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence.UserDerivedIndicatorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UserDerivedIndicator 도메인 ↔ Entity 매퍼.
 * 수식 직렬화는 DerivedFormulaJsonConverter에 위임(Entity는 String만 보유).
 */
@Component
@RequiredArgsConstructor
public class UserDerivedIndicatorMapper {

    private final DerivedFormulaJsonConverter formulaJsonConverter;

    public UserDerivedIndicatorEntity toEntity(UserDerivedIndicator domain) {
        return new UserDerivedIndicatorEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getName(),
                domain.getUnit(),
                domain.getDescription(),
                formulaJsonConverter.toJson(domain.getFormula()),
                domain.getCategory(),
                domain.getCreatedAt()
        );
    }

    public UserDerivedIndicator toDomain(UserDerivedIndicatorEntity entity) {
        return new UserDerivedIndicator(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getUnit(),
                entity.getDescription(),
                formulaJsonConverter.fromJson(entity.getFormulaJson()),
                entity.getCategory(),
                entity.getCreatedAt()
        );
    }
}
