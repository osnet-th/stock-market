package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsEventCategoryJpaRepository extends JpaRepository<NewsEventCategoryEntity, Long> {

    Optional<NewsEventCategoryEntity> findByUserIdAndName(Long userId, String name);

    Optional<NewsEventCategoryEntity> findByIdAndUserId(Long id, Long userId);

    List<NewsEventCategoryEntity> findByUserIdOrderByNameAsc(Long userId);
}