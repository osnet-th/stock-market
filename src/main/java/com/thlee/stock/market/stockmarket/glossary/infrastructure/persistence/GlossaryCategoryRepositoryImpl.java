package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryCategoryRepository;
import com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.mapper.GlossaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GlossaryCategory 포트 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class GlossaryCategoryRepositoryImpl implements GlossaryCategoryRepository {

    private final GlossaryCategoryJpaRepository jpaRepository;

    @Override
    public GlossaryCategory save(GlossaryCategory category) {
        GlossaryCategoryEntity entity = GlossaryMapper.toEntity(category);
        GlossaryCategoryEntity saved = jpaRepository.save(entity);
        if (category.getId() == null) {
            category.assignId(saved.getId());
        }
        return GlossaryMapper.toDomain(saved);
    }

    @Override
    public Optional<GlossaryCategory> findByUserIdAndName(Long userId, String name) {
        return jpaRepository.findByUserIdAndName(userId, name).map(GlossaryMapper::toDomain);
    }

    @Override
    public Optional<GlossaryCategory> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(GlossaryMapper::toDomain);
    }

    @Override
    public List<GlossaryCategory> findByUserIdOrderByNameAsc(Long userId) {
        return jpaRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(GlossaryMapper::toDomain)
                .toList();
    }

    @Override
    public int deleteByIdAndUserId(Long id, Long userId) {
        return jpaRepository.deleteByIdAndUserId(id, userId);
    }
}