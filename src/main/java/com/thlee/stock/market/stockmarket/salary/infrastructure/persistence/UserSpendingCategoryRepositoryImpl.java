package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.UserSpendingCategory;
import com.thlee.stock.market.stockmarket.salary.domain.repository.UserSpendingCategoryRepository;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper.UserSpendingCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserSpendingCategoryRepositoryImpl implements UserSpendingCategoryRepository {

    private final UserSpendingCategoryJpaRepository jpaRepository;

    @Override
    public UserSpendingCategory save(UserSpendingCategory category) {
        UserSpendingCategoryEntity saved = jpaRepository.save(UserSpendingCategoryMapper.toEntity(category));
        return UserSpendingCategoryMapper.toDomain(saved);
    }

    @Override
    public List<UserSpendingCategory> saveAll(List<UserSpendingCategory> categories) {
        return jpaRepository.saveAll(categories.stream()
                        .map(UserSpendingCategoryMapper::toEntity)
                        .collect(Collectors.toList())).stream()
                .map(UserSpendingCategoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSpendingCategory> findAllByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .map(UserSpendingCategoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserSpendingCategory> findByUserIdAndCode(Long userId, String code) {
        return jpaRepository.findByUserIdAndCode(userId, code)
                .map(UserSpendingCategoryMapper::toDomain);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }
}
