package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;

import java.util.List;

public interface KeywordService {

    /**
     * 키워드 등록
     */
    Keyword registerKeyword(String keyword, Long userId);

    /**
     * 사용자별 키워드 목록 조회
     */
    List<Keyword> getKeywordsByUserId(Long userId);

    /**
     * 사용자별 활성화된 키워드 목록 조회
     */
    List<Keyword> getActiveKeywordsByUserId(Long userId);

    /**
     * 활성화된 모든 키워드 조회 (스케줄러용)
     */
    List<Keyword> getAllActiveKeywords();

    /**
     * 키워드 비활성화
     */
    void deactivateKeyword(Long keywordId);

    /**
     * 키워드 활성화
     */
    void activateKeyword(Long keywordId);

    /**
     * 키워드 삭제
     */
    void deleteKeyword(Long keywordId);
}
