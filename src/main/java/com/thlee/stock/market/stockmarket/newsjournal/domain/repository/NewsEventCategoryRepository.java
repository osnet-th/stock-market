package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;

import java.util.List;
import java.util.Optional;

/**
 * 뉴스 저널 사건 분류 Repository 포트.
 *
 * <p>모든 조회/저장은 {@code userId} 스코프로 제한된다 (소유권 격리).
 */
public interface NewsEventCategoryRepository {

    NewsEventCategory save(NewsEventCategory category);

    Optional<NewsEventCategory> findByUserIdAndName(Long userId, String name);

    Optional<NewsEventCategory> findByIdAndUserId(Long id, Long userId);

    /** 사용자 카테고리 목록 (탭/자동완성/응답 동봉용). 이름 오름차순. */
    List<NewsEventCategory> findByUserIdOrderByNameAsc(Long userId);
}