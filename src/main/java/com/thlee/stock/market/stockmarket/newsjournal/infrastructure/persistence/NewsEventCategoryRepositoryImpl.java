package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryRepository;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.mapper.NewsEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * NewsEventCategory 포트 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class NewsEventCategoryRepositoryImpl implements NewsEventCategoryRepository {

    private final NewsEventCategoryJpaRepository jpaRepository;

    @Override
    public NewsEventCategory save(NewsEventCategory category) {
        NewsEventCategoryEntity entity = NewsEventMapper.toEntity(category);
        NewsEventCategoryEntity saved = jpaRepository.save(entity);
        if (category.getId() == null) {
            category.assignId(saved.getId());
        }
        return NewsEventMapper.toDomain(saved);
    }

    @Override
    public Optional<NewsEventCategory> findByUserIdAndName(Long userId, String name) {
        return jpaRepository.findByUserIdAndName(userId, name).map(NewsEventMapper::toDomain);
    }

    @Override
    public Optional<NewsEventCategory> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(NewsEventMapper::toDomain);
    }

    @Override
    public List<NewsEventCategory> findByUserIdOrderByNameAsc(Long userId) {
        return jpaRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(NewsEventMapper::toDomain)
                .toList();
    }
}