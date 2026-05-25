package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.repository.UserDerivedIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence.mapper.UserDerivedIndicatorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDerivedIndicatorRepositoryImpl implements UserDerivedIndicatorRepository {

    private final UserDerivedIndicatorJpaRepository jpaRepository;
    private final UserDerivedIndicatorMapper mapper;

    @Override
    public UserDerivedIndicator save(UserDerivedIndicator indicator) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(indicator)));
    }

    @Override
    public List<UserDerivedIndicator> findByUserId(Long userId) {
        // 손상 JSON 등 매핑 실패 행은 목록 전체를 깨뜨리지 않도록 skip + 경고(graceful).
        List<UserDerivedIndicator> result = new ArrayList<>();
        for (UserDerivedIndicatorEntity entity : jpaRepository.findByUserId(userId)) {
            try {
                result.add(mapper.toDomain(entity));
            } catch (RuntimeException e) {
                log.warn("파생지표 매핑 실패로 목록에서 제외: id={}, userId={}, cause={}",
                        entity.getId(), userId, e.getMessage());
            }
        }
        return result;
    }

    @Override
    public Optional<UserDerivedIndicator> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public int deleteByIdAndUserId(Long id, Long userId) {
        return jpaRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndName(Long userId, String name) {
        return jpaRepository.existsByUserIdAndName(userId, name);
    }
}
