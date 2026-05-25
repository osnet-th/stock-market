package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.repository.UserDerivedIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence.mapper.UserDerivedIndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
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
