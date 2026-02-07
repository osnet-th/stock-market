package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;

import java.util.List;
import java.util.Optional;

/**
 * 키워드 Repository 인터페이스
 * 도메인 계층에 정의하여 인프라 계층이 구현
 */
public interface KeywordRepository {

    /**
     * 키워드 저장
     */
    Keyword save(Keyword keyword);

    /**
     * ID로 키워드 조회
     */
    Optional<Keyword> findById(Long id);

    /**
     * 사용자별 키워드 목록 조회
     */
    List<Keyword> findByUserId(Long userId);

    /**
     * 사용자별 + 활성화 상태별 키워드 목록 조회
     */
    List<Keyword> findByUserIdAndActive(Long userId, boolean active);

    /**
     * 활성화된 모든 키워드 조회 (스케줄러용)
     */
    List<Keyword> findByActive(boolean active);

    /**
     * 키워드 삭제
     */
    void delete(Keyword keyword);
}