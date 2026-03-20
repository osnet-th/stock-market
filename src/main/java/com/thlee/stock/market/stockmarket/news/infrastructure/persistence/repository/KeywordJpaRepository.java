package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.KeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Keyword JPA Repository
 */
public interface KeywordJpaRepository extends JpaRepository<KeywordEntity, Long> {

    Optional<KeywordEntity> findByKeywordAndRegion(String keyword, Region region);

    boolean existsByKeywordAndRegion(String keyword, Region region);
}
