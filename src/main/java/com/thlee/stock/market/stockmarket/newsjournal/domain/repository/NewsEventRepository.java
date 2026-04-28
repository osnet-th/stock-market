package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;

import java.util.List;
import java.util.Optional;

/**
 * 뉴스 저널 사건 본체 Repository 포트.
 *
 * <p>모든 조회는 {@code userId} 스코프로 제한되며, IDOR 방지를 위해
 * {@link #findByIdAndUserId} 패턴을 강제한다.
 */
public interface NewsEventRepository {

    NewsEvent save(NewsEvent event);

    /** 권한이 있는 사용자만 조회 가능. 404 통일을 위해 Optional 반환. */
    Optional<NewsEvent> findByIdAndUserId(Long id, Long userId);

    /** 필터/페이징 리스트. 구현 레이어는 복합 인덱스 {@code (user_id, occurred_date DESC)} 를 활용한다. */
    List<NewsEvent> findList(Long userId, NewsEventListFilter filter);

    long countList(Long userId, NewsEventListFilter filter);

    void deleteByIdAndUserId(Long id, Long userId);
}