package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.repository;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 커스텀 파생지표 저장소 (도메인 포트).
 * 모든 단건 접근은 (id, userId) 복합 조건으로 소유권을 강제한다(IDOR 방지).
 */
public interface UserDerivedIndicatorRepository {

    UserDerivedIndicator save(UserDerivedIndicator indicator);

    List<UserDerivedIndicator> findByUserId(Long userId);

    Optional<UserDerivedIndicator> findByIdAndUserId(Long id, Long userId);

    int deleteByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
