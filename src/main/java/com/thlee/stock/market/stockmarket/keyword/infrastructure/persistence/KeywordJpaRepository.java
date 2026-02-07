package com.thlee.stock.market.stockmarket.keyword.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Keyword JPA Repository
 */
public interface KeywordJpaRepository extends JpaRepository<KeywordEntity, Long> {

    /**
     * 사용자별 키워드 목록 조회
     */
    List<KeywordEntity> findByUserId(Long userId);

    /**
     * 사용자별 + 활성화 상태별 키워드 목록 조회
     */
    List<KeywordEntity> findByUserIdAndActive(Long userId, boolean active);

    /**
     * 활성화 상태별 키워드 목록 조회 (스케줄러용)
     */
    List<KeywordEntity> findByActive(boolean active);
}